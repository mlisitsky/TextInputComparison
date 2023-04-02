package ca.yorku.eecs.textinputcomparison;

import static java.security.AccessController.getContext;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

/*
Many thanks to I. Scott MacKenzie and R. William Soukoreff for creating the phrase set used here.
For more information on the creation of this phrase set, you can read their paper on it here:
http://www.yorku.ca/mack/chi03b.html
 */

public class TestActivity extends Activity {

    private final static String MYDEBUG = "MYDEBUG";
    private final static String PHRASES_LOCATION = "R.raw.phrases";
    private final static String CURRENT_QUESTION_NUMBER = "current_question_number";
    private final static String PHRASES_LIST = "phrases_list";
    private final static String PHRASE_LIST_NUMBER_OF_CHARACTERS = "phraseList_total_characters";
    private final static String CURRENT_PHASE = "current_phase";
    private final static String CURRENT_PHASE_START_TIME = "current_phase_start_time";
    private final static String CURRENT_PHASE_NUMBER_OF_ERRORS = "current_errors";

    private final static int HAPTIC_OFF = 0;
    private final static String HAPTIC_OFF_START_TIME = "haptic_off_start_time";
    private final static String HAPTIC_OFF_FINISH_TIME = "haptic_off_finish_time";
    private final static String HAPTIC_OFF_NUMBER_OF_ERRORS = "haptic_off_errors";
    private final static String HAPTIC_OFF_WPM = "haptic_off_wpm";
    private final static String HAPTIC_OFF_ERROR_RATE = "haptic_off_error_rate";


    private final static int HAPTIC_ON = 1;
    private final static String HAPTIC_ON_START_TIME = "haptic_on_start_time";
    private final static String HAPTIC_ON_FINISH_TIME = "haptic_on_finish_time";
    private final static String HAPTIC_ON_NUMBER_OF_ERRORS= "haptic_on_errors";
    private final static String HAPTIC_ON_WPM = "haptic_on_wpm";
    private final static String HAPTIC_ON_ERROR_RATE = "haptic_on_error_rate";

    private final static int VOICE_RECOGNITION = 2;
    private final static String VOICE_RECOGNITION_START_TIME = "voice_recognition_start_time";
    private final static String VOICE_RECOGNITION_FINISH_TIME = "voice_recognition_finish_time";
    private final static String VOICE_RECOGNITION_NUMBER_OF_ERRORS = "voice_recognition_errors";
    private final static String VOICE_RECOGNITION_WPM = "voice_recognition_wpm";
    private final static String VOICE_RECOGNITION_ERROR_RATE = "voice_recognition_error_rate";

    private final static int NUMBER_OF_QUESTIONS = 2;

    int phraseListTotalCharacters;
    int currentQuestionNumber, currentErrors, currentPhase;
    long currentStartTime;
    boolean errorFound, processingEntry, phaseOver;
    int hapticOffErrors, hapticOnErrors, voiceRecognitionErrors;
    long hapticOffStartTime, hapticOffFinishTime, hapticOnStartTime, hapticOnFinishTime, voiceRecognitionStartTime, voiceRecognitionFinishTime;
    float hapticOffWPM, hapticOffErrorRate, hapticOnWPM, hapticOnErrorRate, voiceRecognitionWPM, voiceRecognitionErrorRate;
    ArrayList<String> testPhraseList;
    TextView text_to_type;
    EditText input_field;
    Button nextPhaseButton;
    Vibrator vib;
    ToneGenerator toneGenerator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);

        text_to_type = findViewById(R.id.text_to_type);
        input_field = findViewById(R.id.input_field);
        nextPhaseButton = findViewById(R.id.next_phase_button);
        nextPhaseButton.setVisibility(View.GONE);

        phraseListTotalCharacters = 0;
        testPhraseList = generatePhraseSet();
        for (String s : testPhraseList) {
            phraseListTotalCharacters += s.length();
        }

        text_to_type.setText(testPhraseList.get(currentQuestionNumber));

        // Turn haptic feedback off
        View view = findViewById(android.R.id.content);
        view.setHapticFeedbackEnabled(false);

        errorFound = false;
        processingEntry = false;
        phaseOver = false;
        currentPhase = HAPTIC_OFF;
        currentErrors = 0;
        currentStartTime = System.currentTimeMillis();

        hapticOffStartTime = 0;
        hapticOffFinishTime = 0;
        hapticOffErrors = 0;

        hapticOnStartTime = 0;
        hapticOnFinishTime = 0;
        hapticOnErrors = 0;

        voiceRecognitionStartTime = 0;
        voiceRecognitionFinishTime = 0;
        voiceRecognitionErrors = 0;

        hapticOffWPM = 0f;
        hapticOffErrorRate = 0f;
        hapticOnWPM = 0f;
        hapticOnErrorRate = 0f;
        voiceRecognitionWPM = 0f;
        voiceRecognitionErrorRate = 0f;

        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        input_field.addTextChangedListener(new userInputListener());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Phrase List Info
        testPhraseList = savedInstanceState.getStringArrayList(PHRASES_LIST);
        phraseListTotalCharacters = savedInstanceState.getInt(PHRASE_LIST_NUMBER_OF_CHARACTERS);

        // Current Phase
        currentPhase = savedInstanceState.getInt(CURRENT_PHASE);
        currentQuestionNumber = savedInstanceState.getInt(CURRENT_QUESTION_NUMBER);
        currentStartTime = savedInstanceState.getLong(CURRENT_PHASE_START_TIME);
        currentErrors = savedInstanceState.getInt(CURRENT_PHASE_NUMBER_OF_ERRORS);

        // Haptics Off Phase
        hapticOffStartTime = savedInstanceState.getLong(HAPTIC_OFF_START_TIME);
        hapticOffFinishTime = savedInstanceState.getLong(HAPTIC_OFF_FINISH_TIME);
        hapticOffErrors = savedInstanceState.getInt(HAPTIC_OFF_NUMBER_OF_ERRORS);

        // Haptics On Phase
        hapticOnStartTime = savedInstanceState.getLong(HAPTIC_ON_START_TIME);
        hapticOnFinishTime = savedInstanceState.getLong(HAPTIC_ON_FINISH_TIME);
        hapticOnErrors = savedInstanceState.getInt(HAPTIC_OFF_NUMBER_OF_ERRORS);

        // Voice Recognition Phase
        voiceRecognitionStartTime = savedInstanceState.getLong(VOICE_RECOGNITION_START_TIME);
        voiceRecognitionFinishTime = savedInstanceState.getLong(VOICE_RECOGNITION_FINISH_TIME);
        voiceRecognitionErrors = savedInstanceState.getInt(VOICE_RECOGNITION_NUMBER_OF_ERRORS);

    }

    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Phrase List Info
        savedInstanceState.putStringArrayList(PHRASES_LIST, testPhraseList);
        savedInstanceState.putInt(PHRASE_LIST_NUMBER_OF_CHARACTERS, phraseListTotalCharacters);

        // Current Phase
        savedInstanceState.putInt(CURRENT_PHASE,currentPhase);
        savedInstanceState.putInt(CURRENT_QUESTION_NUMBER,currentQuestionNumber);
        savedInstanceState.putLong(CURRENT_PHASE_START_TIME, currentStartTime);
        savedInstanceState.putInt(CURRENT_PHASE_NUMBER_OF_ERRORS, currentErrors);

        // Haptics Off Phase
        savedInstanceState.putLong(HAPTIC_OFF_START_TIME, hapticOffStartTime);
        savedInstanceState.putLong(HAPTIC_OFF_FINISH_TIME, hapticOffFinishTime);
        savedInstanceState.putInt(HAPTIC_OFF_NUMBER_OF_ERRORS, hapticOffErrors);

        // Haptics On Phase
        savedInstanceState.putLong(HAPTIC_ON_START_TIME, hapticOnStartTime);
        savedInstanceState.putLong(HAPTIC_ON_FINISH_TIME, hapticOnFinishTime);
        savedInstanceState.putInt(HAPTIC_ON_NUMBER_OF_ERRORS, hapticOnErrors);

        // Voice Recognition Phase
        savedInstanceState.putLong(VOICE_RECOGNITION_START_TIME, voiceRecognitionStartTime);
        savedInstanceState.putLong(VOICE_RECOGNITION_FINISH_TIME, voiceRecognitionFinishTime);
        savedInstanceState.putInt(VOICE_RECOGNITION_NUMBER_OF_ERRORS, voiceRecognitionErrors);

        super.onSaveInstanceState(savedInstanceState);
    }


    protected ArrayList<String> generatePhraseSet() {
        ArrayList<String> fullPhraseList = new ArrayList<>();
        ArrayList<String> curatedPhrases = new ArrayList<>();

        Resources resources = getApplicationContext().getResources();
        InputStream inputStream = resources.openRawResource(R.raw.phrases);

        currentQuestionNumber = 0;
        try {
//            FileReader fReader = new FileReader(PHRASES_LOCATION);
            InputStreamReader iReader = new InputStreamReader(inputStream);
            BufferedReader bReader = new BufferedReader(iReader);
            String line;

            // Add each line of the initial phrase list text file to phrases as a String
            while ((line = bReader.readLine()) != null) {
                fullPhraseList.add(line);
            }
            iReader.close();

            Random rand = new Random();

            // Pull ten random items from the initial phrase list and add them to test phrase list
            for (int i = 0; i < NUMBER_OF_QUESTIONS; i++) {
                int listItemNumber = rand.nextInt(fullPhraseList.size() - 1);
                curatedPhrases.add(fullPhraseList.get(listItemNumber));
            }
        }
        catch (FileNotFoundException e) {
            Log.i(MYDEBUG, "File not found.");
        }
        catch (IOException e) {
            Log.i(MYDEBUG, "IOException");
        }
        return curatedPhrases;
    }

    protected float calculateWPM(long millisTime) {
        long secondsTime = millisTime/1000;

        return (secondsTime / 60) / NUMBER_OF_QUESTIONS;
    }

    protected float calculateErrorRate(int errors) {
        int totalCharactersTyped = 0;

        for (String s : testPhraseList) {
            totalCharactersTyped += s.length();
        }

        return 100f*(errors/totalCharactersTyped);
    }

    protected void phaseChange() {
        Log.i(MYDEBUG, "Phase Change! ");
//        currentQuestionNumber = 0;
        nextPhaseButton.setVisibility(View.VISIBLE);
        input_field.setVisibility(View.GONE);
        input_field.setEnabled(false);
        text_to_type.setText(R.string.next_phase_warning_text);

        if (currentPhase == HAPTIC_OFF) {
            hapticOffFinishTime = System.currentTimeMillis();
            hapticOffStartTime = currentStartTime;
            hapticOffErrors = currentErrors;
            currentErrors = 0;
            currentStartTime = System.currentTimeMillis();
            currentPhase = HAPTIC_ON;
            // Turn haptic feedback on
            View view = findViewById(android.R.id.content);
            view.setHapticFeedbackEnabled(true);
        } else if (currentPhase == HAPTIC_ON) {
            hapticOnFinishTime = System.currentTimeMillis();
            hapticOnStartTime = currentStartTime;
            hapticOnErrors = currentErrors;
            currentErrors = 0;
            currentStartTime = System.currentTimeMillis();
            currentPhase = VOICE_RECOGNITION;

            // Turn haptic feedback off again
            View view = findViewById(android.R.id.content);
            view.setHapticFeedbackEnabled(false);
            // Enable Voice Recognition instead of keyboard here
        } else if (currentPhase == VOICE_RECOGNITION){
            voiceRecognitionFinishTime = System.currentTimeMillis();
            voiceRecognitionStartTime = currentStartTime;
            voiceRecognitionErrors = currentErrors;

            hapticOffWPM = calculateWPM(hapticOffFinishTime-hapticOffStartTime);
            hapticOffErrorRate = calculateErrorRate(hapticOffErrors);
            hapticOnWPM = calculateWPM(hapticOnFinishTime-hapticOnStartTime);
            hapticOnErrorRate = calculateErrorRate(hapticOnErrors);
            voiceRecognitionWPM = calculateWPM(voiceRecognitionFinishTime-voiceRecognitionStartTime);
            voiceRecognitionErrorRate = calculateErrorRate(voiceRecognitionErrors);

            getResults();
        }
    }

    public void clickNextPhase(View view) {
        text_to_type.setText(testPhraseList.get(currentQuestionNumber));
        input_field.setVisibility(View.VISIBLE);
        input_field.setEnabled(true);
        nextPhaseButton.setVisibility(View.GONE);
        input_field.getText().clear();
    }

    public void getResults() {
        Intent i = new Intent(getApplicationContext(), ResultsActivity.class);
        Bundle b = new Bundle();

        b.putFloat(HAPTIC_OFF_WPM, hapticOffWPM);
        b.putFloat(HAPTIC_OFF_ERROR_RATE, hapticOffErrorRate);
        b.putFloat(HAPTIC_ON_WPM, hapticOnWPM);
        b.putFloat(HAPTIC_ON_ERROR_RATE, hapticOnErrorRate);
        b.putFloat(VOICE_RECOGNITION_WPM, voiceRecognitionWPM);
        b.putFloat(VOICE_RECOGNITION_ERROR_RATE, voiceRecognitionErrorRate);
        i.putExtras(b);

        startActivity(i);

        finish();
    }

    // ==================================================================================================
    private class userInputListener implements TextWatcher {

        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }


        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        /*
            Get the phrase that the user is currently trying to type. Then get the index of the character they are trying to type.
         */

            String currentQuestionPhrase = testPhraseList.get(currentQuestionNumber);
            int indexOfTypedCharacter = s.length() - 1;
            if (indexOfTypedCharacter < 0) {
                indexOfTypedCharacter = 0;
            }

            /*
                Check to make sure the user has not finished typing the phrase already. Otherwise, go to next phrase in the list.
                If there are no more words in the list, change to next phase.
             */
            if (testPhraseList.isEmpty() || currentQuestionNumber >= testPhraseList.size()) {
                Log.i(MYDEBUG, "Invalid currentQuestionNumber or testPhraseList is empty.");
                if (testPhraseList.isEmpty()) {
                    Log.e(MYDEBUG, "testPhraseList is empty.");
                }
                if (currentQuestionNumber >= testPhraseList.size()) {
                    Log.e(MYDEBUG, "Invalid currentQuestionNumber ");
                }
            }

            /*
                Stops it from trying to read null characters mid-phase-change
             */
            if (s == null || s.length() <= indexOfTypedCharacter) {
                return;
            }

            if (indexOfTypedCharacter < currentQuestionPhrase.length() - 1) {

                char correctChar = currentQuestionPhrase.charAt(indexOfTypedCharacter);
                Log.i(MYDEBUG, "correctChar: " + correctChar);
                char typedChar = s.charAt(indexOfTypedCharacter);
                Log.i(MYDEBUG, "typedChar: " + typedChar);
             /*
                Check to make sure the user has not finished typing the phrase already. Compare the typed char to the correct answer.
                If typed char is incorrect, then temporarily disable the watcher and send out a beeping sound.
                After that, delete the most recently entered character from the input box and flag that user's most recent input was incorrect.
                If the user's input was correct, remove any flag marking the most recent input as incorrect.
             */

                if (typedChar != correctChar) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                    input_field.removeTextChangedListener(this);
                    Log.i(MYDEBUG, "indexOfTypedCharacter: " + indexOfTypedCharacter);
                    Log.i(MYDEBUG, "currentQuestionPhrase.length(): " + currentQuestionPhrase.length());
                    input_field.setText(currentQuestionPhrase.substring(0, Math.min(indexOfTypedCharacter, currentQuestionPhrase.length())));
                    if (indexOfTypedCharacter < s.length()) {
                        input_field.setSelection(indexOfTypedCharacter);
                    }
                    input_field.addTextChangedListener(this);
                    errorFound = true;
                } else {
                    errorFound = false;
                }
            } else {
                Log.i(MYDEBUG, "1currentQuestionNumber: " + currentQuestionNumber);
                Log.i(MYDEBUG, "1testPhraseList.size(): " + testPhraseList.size());
                currentQuestionNumber++;

                if (currentQuestionNumber <= testPhraseList.size() - 1) {
                    indexOfTypedCharacter = 0;
 //                   String nextQuestionPhrase = testPhraseList.get(currentQuestionNumber);
                    input_field.removeTextChangedListener(this);
//                   if (indexOfTypedCharacter < currentQuestionPhrase.length()) {
                    if (indexOfTypedCharacter < currentQuestionPhrase.length() - 1 && indexOfTypedCharacter < s.length()) {
                        input_field.setSelection(indexOfTypedCharacter);
                    } else {
                        input_field.setSelection(currentQuestionPhrase.length());
                    }
                    input_field.removeTextChangedListener(this);
                    input_field.setText(new Editable.Factory().newEditable(""));
                    text_to_type.setText(testPhraseList.get(currentQuestionNumber));
                    Log.i(MYDEBUG, "changing test to type to: " + testPhraseList.get(currentQuestionNumber));

                    input_field.addTextChangedListener(this);
  //                  text_to_type.setText(nextQuestionPhrase);
 //                   input_field.addTextChangedListener(this);
                } else {
                    Log.i(MYDEBUG, "2currentQuestionNumber: " + currentQuestionNumber);
                    Log.i(MYDEBUG, "2testPhraseList.size(): " + testPhraseList.size());
                    phaseOver = true;
                    errorFound = false;
                    currentQuestionNumber = 0;
                    phaseChange();
                }
                errorFound = false;
            }
        }
    }
}

