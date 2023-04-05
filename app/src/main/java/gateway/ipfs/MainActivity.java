package gateway.ipfs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.web3auth.core.Web3Auth;
import com.web3auth.core.isEmailValid;
import com.web3auth.core.types.*;
import java8.util.concurrent.CompletableFuture;
import org.json.JSONObject;
import web3.ipfsgate.Gater;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multiaddr.MultiAddress;


public class MainActivity extends AppCompatActivity {
    private lateinit var web3Auth: Web3Auth

    private val verifierList: List<LoginVerifier> = listOf(
            LoginVerifier("Google", Provider.GOOGLE),
    LoginVerifier("Facebook", Provider.FACEBOOK),
    LoginVerifier("Twitch", Provider.TWITCH),
    LoginVerifier("Discord", Provider.DISCORD),
    LoginVerifier("Reddit", Provider.REDDIT),
    LoginVerifier("Apple", Provider.APPLE),
    LoginVerifier("Github", Provider.GITHUB),
    LoginVerifier("LinkedIn", Provider.LINKEDIN),
    LoginVerifier("Twitter", Provider.TWITTER),
    LoginVerifier("Line", Provider.LINE),
    LoginVerifier("Hosted Email Passwordless", Provider.EMAIL_PASSWORDLESS)
    )

    private var selectedLoginProvider: Provider = Provider.GOOGLE

    private val gson = Gson()

    private fun signIn() {
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
                var extraLoginOptions: ExtraLoginOptions? = null
        if (selectedLoginProvider == Provider.EMAIL_PASSWORDLESS) {
            val hintEmail = hintEmailEditText.text.toString()
            if (hintEmail.isBlank() || !hintEmail.isEmailValid()) {
                Toast.makeText(this, "Please enter a valid Email.", Toast.LENGTH_LONG).show()
                return
            }
            extraLoginOptions = ExtraLoginOptions(login_hint = hintEmail)
        }

        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.login(
                LoginParams(selectedLoginProvider, extraLoginOptions = extraLoginOptions)
        )
        loginCompletableFuture.whenComplete { loginResponse, error ->
            if (error == null) {
                reRender(loginResponse)
                println("PrivKey: " + web3Auth.getPrivkey())
                println("ed25519PrivKey: " + web3Auth.getEd25519PrivKey())
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }
    }

    private fun signOut() {
        val logoutCompletableFuture = web3Auth.logout()
        logoutCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                reRender(Web3AuthResponse())
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }
    }

    private fun reRender(web3AuthResponse: Web3AuthResponse) {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
                val signInButton = findViewById<Button>(R.id.signInButton)
                val signOutButton = findViewById<Button>(R.id.signOutButton)
                val spinner = findViewById<TextInputLayout>(R.id.verifierList)
                val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)

                val key = web3AuthResponse.privKey
        val userInfo = web3AuthResponse.userInfo
        if (key is String && key.isNotEmpty()) {
            val jsonObject = JSONObject(gson.toJson(web3AuthResponse))
            contentTextView.text = jsonObject.toString(4)
            contentTextView.movementMethod = ScrollingMovementMethod()
            contentTextView.visibility = View.VISIBLE
            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
            spinner.visibility = View.GONE
            hintEmailEditText.visibility = View.GONE
        } else {
            contentTextView.text = getString(R.string.not_logged_in)
            contentTextView.visibility = View.GONE
            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
            spinner.visibility = View.VISIBLE
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure Web3Auth
        web3Auth = Web3Auth(
                Web3AuthOptions(
                        context = this,
                        clientId = getString(R.string.web3auth_project_id),
                        network = Web3Auth.Network.MAINNET,
                        redirectUrl = Uri.parse("torusapp://org.torusresearch.web3authexample/redirect"),
                        whiteLabel = WhiteLabelData(
                                "Web3Auth Sample App", null, null, "en", true,
                                hashMapOf(
                                        "primary" to "#123456"
                                )
                        ),
                        loginConfig = hashMapOf(
                                "loginConfig" to LoginConfigItem(
                                        "torus",
                                        typeOfLogin = TypeOfLogin.GOOGLE,
                                        name = ""
                                )
                        )
                )
        )

        web3Auth.setResultUrl(intent.data)

        // for session response
        val sessionResponse: CompletableFuture<Web3AuthResponse> = web3Auth.sessionResponse()
        sessionResponse.whenComplete { loginResponse, error ->
            if (error == null) {
                reRender(loginResponse)
                println("PrivKey: " + web3Auth.getPrivkey())
                println("ed25519PrivKey: " + web3Auth.getEd25519PrivKey())
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }

        // Setup UI and event handlers
        val signInButton = findViewById<Button>(R.id.signInButton)
                signInButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
                signOutButton.setOnClickListener { signOut() }

        val spinner = findViewById<AutoCompleteTextView>(R.id.spinnerTextView)
                val loginVerifierList: List<String> = verifierList.map { item ->
                item.name
        }
        val adapter: ArrayAdapter<String> =
        ArrayAdapter(this, R.layout.item_dropdown, loginVerifierList)
        spinner.setAdapter(adapter)
        spinner.onItemClickListener = this
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        web3Auth.setResultUrl(intent?.data)
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedLoginProvider = verifierList[p2].loginProvider

        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        if (selectedLoginProvider == Provider.EMAIL_PASSWORDLESS) {
            hintEmailEditText.visibility = View.VISIBLE
        } else {
            hintEmailEditText.visibility = View.GONE
        }

        //Implementing IPFS with Infura API's
        MultiAddress IPFS_INFURA_URL = new MultiAddress("/dnsaddr/ipfs.infura.io/tcp/5001/https");

        //Creating an IPFS client:
        IPFS ipfsClient;

        //Better to separate declaration and definition as connecting to the client requires a network call
        //In Android, a network call cannot be performed on the main thread
        //Doing so produces a Android.OS.NetworkOnMainThreadException

        new Thread(new Runnable() {
            @Override
            public void run() {
                ipfsClient = new IPFS(IPFS_INFURA_URL);
            }
        }).start();
        //ipfsClient.add() requires two parameters:
        //name-of-file in string format
        //contents-of-file in byte[] format

        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
        try {
            MerkleNode addResult = ipfs.add(file).get(0);

            //To retrieve the document hash:
            Log.i("IPFS Document Hash: ",addResult.hash.toBase58());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gater.simpleText(this, "SDK is on");
    }

}
