# Team Project

Please keep this up-to-date with information about your project throughout the term.

The readme should include information such as:
- a summary of what your application is all about
- a list of the user stories, along with who is responsible for each one
- information about the API(s) that your project uses 
- screenshots or animations demonstrating current functionality

By keeping this README up-to-date,
your team will find it easier to prepare for the final presentation
at the end of the term.


Team Information 

Team Members 

    Mithuran Mathurangan 

    Lee Tak Hei 

    Runyu Tian

    EdwinYichun Deng

Project Description 

Domain 

Stock analysis platform using Alpaca market data.

 

User Stories 

    Detailed analysis of 1 stock 

    Comparison of 2 stocks 

    Watch list for relevant changes to any given stock 

    Investment portfolio  

 

Use Cases 

 

    1.  

    Input a stock and get the relevant analysis as an output.  

    The user will have access to a search bar with a drop-down menu  

    The analysis comes in the form of graphs for historical data and lists for other data.  

    If the user inputs a name that isn’t in the stock list, they get an error 

 

    2.  

    Input 2 stocks and get the comparison.  

    The comparison shows the respective price history, any correlations if present, and any projected prices.  

 

    3.  

    The watchlist saves given stocks to files on the user’s account. 

    From there, the user can open the program whenever they want, and the program will pull up any recent changes to the saved stocks in their watchlist.  

    4.  

    A platform for the user to use fake money to simulate the process of buying the stock.  

    The program will increase or decrease their money based off of how their stocks of choice do in real time.   

    The user can also buy/sell their stocks when they choose 

    The user will have access to their portfolio performance  

    If extra time is available, this can be changed to a proper portfolio timeline 

 

 

Minimal Viable Product (MVP) 


Investment porfoilo
Feature	MVP behaviour
Initial balance	Give each user $10,000 in virtual cash
Buy stocks	Enter a ticker and quantity; buy at the current market price
Sell stocks	Sell shares that the user currently owns
Available cash	Display remaining virtual cash
Holdings	Show each stock, quantity, average purchase price and current price
Portfolio value	Cash + current market value of all holdings
Profit/loss	Current portfolio value − initial $10,000
Transaction history	Record each buy/sell, quantity, price and timestamp
Persistence	Save and reload the portfolio when the user logs in again

Add team member who does what user story here

 

Entities 

    - Stocks 

    - Portfolios 

    - Watchlists 

    - StockData 

    - Transcations 
    

### Stock comparison data

The comparison supports U.S. stock symbols available on Alpaca’s IEX feed.
Click either stock field to browse the directory. Type a ticker such as `NVDA`
or a company name such as `Nvidia` to narrow the list. Each result shows its
ticker and name. Click a result or use the arrow keys and Enter to choose it.
The live comparison loads the directory in the background when the screen opens
and keeps a local copy for 24 hours. On a later launch, an older saved directory
can be searched while Alpaca supplies an update. Prices are fetched separately.
The offline demo offers AAPL, IBM, MSFT, NVDA, and TSLA with synthetic prices
and trade times. [Alpaca asset directory](https://docs.alpaca.markets/us/reference/get-v2-assets-1)

#### Running the comparison in IntelliJ

1. Open the repository’s `pom.xml` as a Maven project with JDK 11 or later.
2. Run `stockprototype.StockComparisonApp` with the program argument `--demo`
   for an offline comparison. Enter NVDA and TSLA and click **Compare**. The
   default period is **60 trading days (about 3 months)**. The dates above the
   chart span the first and last daily closes shared by both stocks in that period.
3. For live mode, generate your own paper-account key ID and secret key.
   Save them on your computer in a private file outside this repository.
   Use the two environment variable names below and put each value after its
   equals sign. [Alpaca setup guide](https://docs.alpaca.markets/us/docs/getting-started-with-alpaca-market-data)
4. In **Run → Edit Configurations**, select the application, set **Program
   arguments** to `--live`, and select your private file in **Environment
   variables**. Click **Apply** and **Run**. The configuration needs only
   the path to that file. [IntelliJ environment variables](https://www.jetbrains.com/help/idea/program-arguments-and-environment-variables.html)

```dotenv
APCA_API_KEY_ID=
APCA_API_SECRET_KEY=
```

For the console demo, run `stockprototype.StockApiDemo` with `--demo NVDA` or
`--live NVDA`. Both live entry points use the same environment variables.

#### What the prices represent

Each click on **Compare** fetches the latest available IEX trade for each stock,
with its timestamp, and compares raw daily closes for shared dates before today
in New York. When a stock has traded today, its chart line extends to a point
labeled with today’s date and **Live**. The legend shows the trade price in dollars.
Click **Compare** again to refresh it. The percentage change, correlation,
closing-price table, and next-close estimate use completed daily closes. The historical
request starts in 2016, or at the stock’s first available price if later.
A stock split can affect comparisons that span the split because the closing
prices are unadjusted. [Alpaca historical bars](https://docs.alpaca.markets/us/reference/stockbars) · [Latest trades](https://docs.alpaca.markets/us/reference/stocklatesttrades-1)

For the shorter bundled example, choose IBM and AAPL, select
**20 trading days (about 1 month)**, and click **Compare**.
IBM rises from 215.35 to 217.86 (+1.17%) and AAPL rises from 110.68 to
114.98 (+3.89%). AAPL leads by 2.72 percentage points. Open **Closing prices**
to inspect the individual dates.

The comparison runs in its own Swing window. `ComparisonInteractor` uses the
`StockDataSource` interface for daily closes and recent trades. The chart,
tables, and summary share the same comparison result.

Design Diagrams 

Add UML Class Diagram 
<img width="5160" height="3732" alt="mermaid-diagram (2)" src="https://github.com/user-attachments/assets/a8ed3c5f-de14-4cc5-83bb-3095ca5d8842" />

Add Sequence Diagrams 

 

How to Run 

Once the program has begun, the user will be given a login screen in order to either sign up or input their login information. Once done, the user will be led to a screen with four options, one for each user story. In the first option, a user can search up a stock and 
see it's analysis. In the same window they will have an option to add the stock to their watch list. The search bar will remain on the screen if the user wishes to search a different stock.
The second button will allow the user to search up two different stocks and see their relevant comparison. The third button will allow the user to view their watch list and see the current status of the stocks on the list. The fourth button will take the user to a screen where 
they can use fake money to simulate buying stocks and view their portfolio performance. On all screens there will be a back button available for if the user wants to go back to the initial screen with the four buttons. 

Notes 

[Any additional project information.] 
Your program must demonstrate some form of data persistence.
This could be storing data through some kind of API, writing data to files, or writing data to some kind of database.
Using the API that is used by the user login system example's DBUserDataAccessObject classLinks to an external site. [updated Nov 7th with version using the correct API url for this term] to store and retrieve a "user" and an associated JSON object could satisfy both of the above requirements for API usage and data persistence. This would be appropriate for projects that rely on being able to share data across multiple users across devices, for example. 
