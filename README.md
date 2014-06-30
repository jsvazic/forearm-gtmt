# Forearm&trade; - <i>Growing The Money Tree</i> Edition

Welcome to Forearm&trade; - *Growing The Money Tree* edition.  This version of the Forearm&trade; software is specifically designed
to represent the trading strategies outlined in Chapter 6 of *Growing The Money Tree*, with the following modifications:

* Trades happen against the M30 time frame, not H1
* The minimum delta between the moving averages is 3.0, not 3.5
* The risk:reward ratio is 1:2 rather than 1:5
  * The SL trigger is set to 30.0 pips instead of  50.0 pips
  * The TP trigger is set to 60.0 pips instead of 250.0 pips
  
All other parameters are the same as outlined in the book.

## About The Project
Forearm&trade; is the name for the Forex trading agent created by John Svazic, author of *Growing The Money Tree*.  This code is made 
available under the BSD license as outlined [here](LICENSE).

I have made this code available to the public in the hopes of raising awareness of how to trade the foreign exchange (Forex) market as well
as sparking interest in self-management of finances.  For more information I would recommend reading (and leaving a review if you would
be so kind) *Growing The Money Tree*, available on Amazon and other great retailers.

## Prerequisites
This project is build for Scala 2.10.2 and uses SBT 0.12.4 to build the project.  As such, the prerequisites for running Forearm&trade; are:

* [Java SDK 7.0](http://www.oracle.com/technetwork/java/index.html)
* [SBT 0.12.4 or later](http://www.scala-sbt.org/)
* An account with [Oanda](http://fxtrade.oanda.com/) with a corresponding API token for the account

**Note:** Java 8 does not work with Scala 2.10.x.  This is a known issue and may be resolved in a later release.

## Configuration
The [ForearmSettings](src/main/scala/com/arm/forearm/ForearmSettings.scala) class is the only file you should need to edit.  Provide the 
account ID and API token as specified.  No other configuration steps are necessary to have the application perform as intended.

## Running The Application
From a command prompt run the following in the root of the Forearm&trade; directory:

        $ sbt clean run
    
All dependencies will automatically download, source code will compile and the application will begin to run.

The application will execute the trading strategy once every 30 minutes at the top and bottom of each hour.  It is highly recommended that
you synchronize your computer's clock with a network time server to ensure maximum efficiency of the algorithm.   

## About The Author
John Svazic is the author of *Growing The Money Tree* and can be found writing blog articles on [growingthemoneytree.com](http://www.growingthemoneytree.com).  
Feel free to stop by the website to find other ways to get in touch with John or to find more information about the book. 

## Disclaimer
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
	IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
	FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
	DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
	SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
	CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
	OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
	OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.