package com.eduardo.android.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Eduardo on 06/03/2016.
 */
public class ForecastFragment extends Fragment {

    // Variável adapter que faz a ligação entre os dados da lista e a view
    ArrayAdapter<String> mPrevisaoAdapter;

    // Construtor default
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetWeatherTask weatherTask = new FetWeatherTask();
            weatherTask.execute("94043");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Método sobrescrito para criar a view w inflar o fragment
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {

        // Lista estática com os dias da semana e a previsão do tempo
        String[] dias = {
                "Seg 22/02 - Ensolarado - 31/27",
                "Ter 23/02 - Nublado - 28/25",
                "Qua 24/25 - Chuva - 26/22",
                "Qui 25/02 - Chuva - 25/23",
                "Sex 26/02 - Ensolarado - 30/26",
                "Sáb 27/02 - Nublado - 27/24",
                "Dom 28/02 - Chuva forte - 22/19"
        };

        // Lista com os dados a exibir na view
        List<String> previsaoSemanal = new ArrayList<String>(Arrays.asList(dias));
        // Inicialização do Adapter com os parâmetros necessários
        mPrevisaoAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_previsao,
                R.id.list_item_previsao_textview,
                previsaoSemanal);
        // Infla o fragment na view
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        // Obtém o listview
        final ListView listView = (ListView) view.findViewById(R.id.listview_previsao);
        // seta o adapter do listview. Em outras palavras, "adapter, pegue esses dados e mostre nessa view".
        listView.setAdapter(mPrevisaoAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Obtém o item selecionado passando a posição como parâmetro de busca
                //String item = listView.getItemAtPosition(position).toString();
                String item = mPrevisaoAdapter.getItem(position);
                // Exibe na Toast o item
                //Toast.makeText(getContext(), item, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, item);
                startActivity(intent);
            }
        });
        // Inicializa uma nova intenção de troca de tela, passando o contexto e a activity a qual deseja mudar


        return view;
    }

    private void updateWeather(){
        FetWeatherTask weatherTask = new FetWeatherTask();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String local = preferences.getString(getString(R.string.pref_local_key),
                getString(R.string.pref_local_default));
        weatherTask.execute(local);
    }

    @Override
    public void onStart(){
        super.onStart();
        updateWeather();
    }



    public class FetWeatherTask extends AsyncTask<String, Void, String[]> {
        private final String LOG_TAG = FetWeatherTask.class.getSimpleName();

        private String getReadableDateString(long time){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd");
            return simpleDateFormat.format(time);
        }

        private String formatHighLows(double high, double low, String unitType){
            if(unitType.equals(getString(R.string.pref_units_imperial))){
                high = (high * 1.8) +  32;
                low = (low * 1.8) + 32;
            } else if(!unitType.equals(getString(R.string.pref_units_metric))){
                Log.d(LOG_TAG, "Unidade não encontrada: " + unitType);
            }

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        private String[] getWeatherDataFromJson(String forecastJsonStr, int days) throws JSONException {
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time();

            String[] resultStr = new String[days];

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = preferences.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric));


            for (int i = 0; i < weatherArray.length(); i++){
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low, unitType);
                resultStr[i] = day + " - " + description + " - " + highAndLow;

            }

            return resultStr;


        }


        protected String[] doInBackground(String... params){

            if(params.length == 0){
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String previsaoJsonStr = null;

            String format = "json";
            String units = "metric";
            int days = 7;
            try{
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri uri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(days))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(uri.toString());

                Log.v(LOG_TAG, "Built URI " + uri.toString());

                // Abre a conexão com a url
                urlConnection = (HttpURLConnection)url.openConnection();
                // Define o método de requisição
                urlConnection.setRequestMethod("GET");
                // Conecta a api
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if(inputStream == null){
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null){
                    buffer.append(line + "\n");
                }
                if(buffer.length() == 0){
                    return null;
                }
                previsaoJsonStr = buffer.toString();

            } catch (IOException e){
                Log.e(LOG_TAG, "Error", e);
                return  null;
            } finally {
                if(urlConnection != null){
                    urlConnection.disconnect();
                }
                if(reader != null){
                    try {
                        reader.close();
                    } catch (final IOException e){
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return  getWeatherDataFromJson(previsaoJsonStr, days);
            } catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result){
            if(result != null){
                mPrevisaoAdapter.clear();
                mPrevisaoAdapter.addAll(result);
            }
        }
    }
}
