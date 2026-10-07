What are you building? One paragraph a non-CS person could understand. Describe the application, not the data structure underneath. 
Black Jack game that shows cards being drawn from a stack, each time the game starts the deck is randomised. The player has a hand and can choose to hold or stand. The player can make a bet at the beginning of a round, a win pays 1:1, a natural blackjack (an ace plus a ten) makes 3:2. The player can double down after seeing their first two cards and may add a second bet equal to the first, then receive another card and stand. 
What is your MVP? The smallest version that actually works, expressed as a set of features. 
A dealer that deals randomized cards, and asks the user whether they want to hit or stay. If the user's cards go over 21, they bust. Whoever is closer to 21 (dealer or user) without going over wins. Win counter tracking the number of losses and wins.


Start a Blackjack session with a standard 52-card deck.
Deal two cards to the player and two cards to the dealer
Allows the player to choose hit/stand
Automatically play the dealers turn based on the basic blackjack rules
Calculate the card values
Determine if the player wins, loses, ties, or busts.
Record every card that has been used in a card-history tracker
Store the result of completed rounds so that the player can review it.
Handle invalid inputs
Player has to type in/deposit money to play, and if the money reaches zero, they cannot play anymore (rejects negative values or bets larger bets than the player’s current balance
Money can keep increasing and keep decreasing


Stretch goals — things you'd add with extra time, separated from the MVP. These are what take you from “Meets” to “Exceeds.” 
Visually representing the cards in the hand with ascii character art on the console
Card counting algorithm to help the player win.
Splitting pairs: if the player has two cards of equal rank, they may post a second bet equal to the first and play two hands, each hand is settled separately against the dealer
Cheating dealer: if the dealer suspects you’re counting they might elect to shuffle after every stand or “peek” the next card using sleight of hand and give you the next one… in the hope that card will be worse for you.
Multiple players able to play at once


What will your LinkedChain hold? Name the two or more element types your generic LinkedChain must store, and why “unordered, duplicates allowed” fits each.
Our linked chain class will hold the deck of cards the game is using in one instance and the game history in another. “Unordered duplicates allowed” makes sense for the history because a win or loss might be recorded multiple times and order does not matter for overall win %. The shoe (the set of cards being played with) has three decks in it, like casinos, so the shoe will by definition contain duplicates. The pop() method we will use to “deal cards” does not respect ordering, so the unordered set makes sense there.  
What bad input must it survive? Name the specific cases — think about what adversarial or careless users might try.
A user should not be able to bet negative money or hit more than there are cards left in the stack. Also if a player does not type in “hit” or “stand” when prompted, they will be prompted again to type in one of the options, rejects negative values or bets larger bets than the player’s current balance
What don't you know how to do yet? Be honest. Naming your unknowns is how you find the right questions to ask your team and GenAI.
We aren’t certain about how to randomize an entire linked chain. We are also not familiar enough with the mathematics of card counting to turn it into a class yet so we will have to study that to implement the stretch goal.

