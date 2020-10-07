package ee.vovtech.backend4cash.service.currency;

import com.mashape.unirest.http.exceptions.UnirestException;
import ee.vovtech.backend4cash.exceptions.CurrencyNotFoundException;
import ee.vovtech.backend4cash.exceptions.InvalidCurrencyException;
import ee.vovtech.backend4cash.model.Currency;
import ee.vovtech.backend4cash.model.CurrencyPrice;
import ee.vovtech.backend4cash.repository.CurrencyRepository;
import ee.vovtech.backend4cash.service.coingecko.CoingeckoAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CurrencyService {

    @Autowired
    private CurrencyRepository currencyRepository;

    public Currency findById(String id) {
        return currencyRepository.findById(id).orElseThrow(CurrencyNotFoundException::new);
    }

    public Currency save(Currency currency) {
        if (currency.getName() == null) {
            throw new InvalidCurrencyException("Currency is undefined");
        }
//        else if (currency.getCurrencyPrice() == null) {
//            throw new InvalidCurrencyException("Currency has no price data");
//        }
        return currencyRepository.save(currency);
    }


    public Currency updateCurrency(String currencyId, Currency currency) {
        if (currencyId == null || currencyRepository.findById(currencyId).isEmpty()) {
            throw new InvalidCurrencyException("Currency is not present in the database");
        } else if (currency.getCurrencyPrice().getDatePriceMap() == null) {
            throw new InvalidCurrencyException("Currency price data is empty");
        } else if (!currencyId.equals(currency.getName())) {
            throw new InvalidCurrencyException("Currencies are different");
        }
        Currency dbCurrency = findById(currencyId);
        dbCurrency.setCurrencyPrice(currency.getCurrencyPrice());
        dbCurrency.setDescription(currency.getDescription());
        dbCurrency.setHomepageLink(currency.getHomepageLink());
        dbCurrency.setImageRef(currency.getImageRef());
        return currencyRepository.save(dbCurrency);
    }

    public void delete(String id) {
        if (findById(id) == null) {
            throw new CurrencyNotFoundException();
        }
        currencyRepository.delete(findById(id));
    }


    public List<Currency> updateCoinsData() throws UnirestException {
        JSONArray coins = CoingeckoAPI.getTopCurrencies(); // get top 10 coins
        for (int i = 0; i < coins.length(); i++) {
            JSONObject coin = coins.getJSONObject(i); // data of each coin
            String name = coin.get("id").toString();
            // if coin doesnt exist, create a new one, else update price data
            if (currencyRepository.findById(name).isEmpty()) {
                createNewCoin(name);
            } else {
                Currency oldCoin = findById(name);
                Map<String, Double> priceData = oldCoin.getCurrencyPrice().getDatePriceMap();
                // currently setting time as 2020-10-07T13:58:37.811Z and price as 10618.69
                priceData.put(coin.getString("last_updated"), coin.getDouble("current_price"));
                // updating price map
                oldCoin.getCurrencyPrice().setDatePriceMap(priceData);
            }
        }
        return currencyRepository.findAll();
    }

    public Currency createNewCoin(String id) throws UnirestException {
        if (currencyRepository.findById(id).isPresent()) { // if the coin is in the db, then there wont be a new one created
            throw new InvalidCurrencyException("Currency already exists");
        }
        JSONObject coin = CoingeckoAPI.getCurrency(id); // get coin data
        Currency newCoin = new Currency(); // put all the data in a new entity
        newCoin.setName(id);
        // TODO fix possible path to homepage
        newCoin.setImageRef(coin.getJSONObject("links").getJSONArray("homepage").get(0).toString());
        // possible TODO add a way to save full description, currently over the char limit(255)
        newCoin.setDescription(id + " with a beautiful description");
        newCoin.setImageRef(coin.getJSONObject("image").get("small").toString());
        newCoin.setCurrencyPrice(null);
        save(newCoin); // save it to the database
        return newCoin;
    }

    public List<Currency> findAll() {
        return currencyRepository.findAll();
    }

}
