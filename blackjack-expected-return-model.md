# Blackjack expected-return model

Before each round of blackjack, this model uses the cards already seen to answer two questions: what is the average result per dollar bet on the next round, and how much should be bet. Every number in the worked example near the end was produced by running the model.

## Rules the model assumes

- A blackjack is an ace plus a ten-value card (ten, jack, queen or king) as a hand's first two cards. It pays 3 to 2.
- The dealer stands on every 17, including a soft 17 (a 17 that counts an ace as 11, such as ace and 6). Row 4 gives the one-line change for tables where the dealer hits soft 17.
- You (the player) can only hit or stand. Doubling, splitting, insurance and surrender are left out.
- Your bet goes down before any card of the round is dealt and cannot change once the deal starts.
- The dealer makes no decisions: below 17 the dealer must draw, and from 17 to 21 the dealer stops.
- Jacks, queens and kings are worth 10, the same as a ten, so the model treats all four as one rank.
- The model values the round as if you were the only player at the table.

### Order of a round

1. You place your bet.
2. You get two cards face up. The dealer gets one card face up and one face down.
3. You hit as many times as you like, until you stand or your total passes 21 (a bust).
4. The dealer turns over the face-down card and draws by the fixed rule.
5. The bet is settled: +1.5 for your blackjack (0 if the dealer also has blackjack), +1 for a win, 0 for a tie (called a push), −1 for a loss. If you bust you lose, even if the dealer busts afterwards.
6. Every card seen during the round is removed from the count of unseen cards, and the next bet is chosen from what remains.

## Why the remaining cards change the answer

You and the dealer draw from the same cards, so a change in the mix would help both sides equally if the rules treated both sides the same. Two rules don't. Your blackjack pays 1.5 times your bet while the dealer's blackjack costs you only your bet, so extra aces and ten-value cards in the remaining cards raise your winnings from blackjacks by more than your losses. And the dealer must hit every total from 12 to 16, where a ten-value card causes a bust, while you may stand on those totals. Remaining cards with few low cards and many tens and aces therefore favour you, and remaining cards rich in low cards favour the dealer. The model computes the size of this effect for the exact cards remaining.

## How to read the equations

Each row gives the rows it depends on, a table of the symbols in its equation, the equation, and an explanation in words. Rows are ordered so that an equation uses only symbols defined in its own row or an earlier one. Each symbol table lists every symbol in that row's equation and nothing else, and symbols defined earlier point back to their row.

The model is built from the dealer's side up to yours, because whether you should hit or stand depends on where the dealer is likely to finish.

---

### Row 1. Hand totals

Uses: nothing earlier.

| Symbol | Meaning |
|---|---|
| $h$ | Hard total: the sum of the hand's cards with every ace counted as 1 and every ten, jack, queen and king as 10. Ace and 6: $h = 7$. |
| $a$ | 1 if the hand holds at least one ace, 0 if not. Ace and 6: $a = 1$. |
| $t(h,a)$ | The total the game scores. Ace and 6: $t = 17$. Ace, 6 and 9: $h = 16$ and $t = 16$, because counting the ace as 11 would make 26. |

```math
\begin{aligned}
&\text{If } a=1 \text{ and } h+10\le 21: && t(h,a)=h+10\\
&\text{Otherwise:} && t(h,a)=h
\end{aligned}
```

Add the cards counting every ace as 1. If the hand has an ace and counting one ace as 11 (adding 10) keeps the total at 21 or below, count it as 11; the hand is then called soft. Only one ace can count as 11, since two would make at least 22. A hand busts exactly when its hard total passes 21, because the extra 10 is only added when it can't cause a bust. The same rule scores the dealer's hand.

### Row 2. The unseen cards

Uses: nothing earlier.

| Symbol | Meaning |
|---|---|
| $r$ | A card rank, written as its value from 1 to 10. 1 is an ace; 10 is a ten, jack, queen or king. |
| $n_r$ | How many cards of rank $r$ you haven't seen. |
| $\mathbf n$ | The ten counts written in order, aces first. A fresh single deck is $(4,4,4,4,4,4,4,4,4,16)$, since tens, jacks, queens and kings together make 16. |
| $\sum_{r=1}^{10}$ | Add the term that follows for $r = 1, 2, \dots, 10$. |
| $\lvert\mathbf n\rvert$ | Total unseen cards. Fresh single deck: 52. |
| $P(\ldots)$ | The chance that the event in brackets happens. |

```math
\lvert\mathbf n\rvert=\sum_{r=1}^{10} n_r
\qquad\qquad
P(\text{next card is rank } r)=\frac{n_r}{\lvert\mathbf n\rvert}
```

"Unseen" means every card whose rank you haven't observed: cards still in the shoe (the box the dealer deals from), cards discarded face down after the shuffle, cards behind the cut card (the plastic card that marks when the dealer reshuffles), and the dealer's face-down card. Shuffling makes every unseen card equally likely to be next, so a rank's chance of coming next is its unseen count divided by the unseen total. In a fresh deck a ten-value card comes next with chance 16 in 52, about 31%.

### Row 3. The dealer's face-up card

Uses: row 2.

| Symbol | Meaning |
|---|---|
| $u$ | The rank of the dealer's face-up card, written 1 to 10 as in row 2. |
| $k(u)$ | The rank that would give the dealer blackjack with this face-up card, or "none". |

```math
\begin{aligned}
&\text{If } u=1: && k(u)=10\\
&\text{If } u=10: && k(u)=1\\
&\text{Otherwise:} && k(u)=\text{none}
\end{aligned}
```

With an ace showing, a ten-value card underneath completes a dealer blackjack. With a ten showing, an ace does. Any other face-up card can't become blackjack.

### Row 4. The dealer drawing to a finish

Uses: rows 1 and 2.

| Symbol | Meaning |
|---|---|
| $x$ | A dealer finish other than blackjack: 17, 18, 19, 20, 21 or bust. |
| $d$, $b$ | The dealer's hard total and ace indicator, worked out the way $h$ and $a$ are in row 1. |
| $F(x \mid d,b,\mathbf n)$ | The chance the dealer finishes at $x$, given that the dealer holds two or more cards with hard total $d$ and ace indicator $b$, and the unseen cards are $\mathbf n$. The bar reads "given": the outcome is on its left and the situation on its right. |
| $[\ldots]$ | 1 if the statement in brackets is true, 0 if false. In the first two cases below it turns a finish that is already certain into a chance of 1 or 0. A dealer past 21 has $F(\text{bust} \mid \ldots) = [\text{bust} = \text{bust}] = 1$ and $F(18 \mid \ldots) = [18 = \text{bust}] = 0$. |
| $b'$ | The dealer's ace indicator after drawing rank $r$: 1 if the dealer already had an ace or the new card is one. |
| $\max(p, q)$ | The larger of $p$ and $q$. |
| $\mathbf n^{-r}$ | The unseen cards after one card of rank $r$ is drawn: the same counts with $n_r$ lowered by 1. Fresh deck after a 3: $(4,4,3,4,4,4,4,4,4,16)$. |
| $t$ | Row 1. |
| $r$, $n_r$, $\mathbf n$, $\lvert\mathbf n\rvert$, $\sum$ | Row 2. |

```math
\begin{aligned}
&\text{If } d>21: && F(x\mid d,b,\mathbf n)=[x=\text{bust}]\\[6pt]
&\text{If } 17\le t(d,b)\le 21: && F(x\mid d,b,\mathbf n)=[x=t(d,b)]\\[6pt]
&\text{If } t(d,b)<17: && F(x\mid d,b,\mathbf n)=\sum_{r=1}^{10}\frac{n_r}{\lvert\mathbf n\rvert}\,F\bigl(x\mid d+r,\ b',\ \mathbf n^{-r}\bigr)\\[6pt]
&\text{where} && b'=\max\bigl(b,[r=1]\bigr)
\end{aligned}
```

The dealer makes no choices. Past 21 the dealer has bust. A total from 17 to 21 stops, so that total is the finish. Below 17 the dealer must draw: for each rank, add it to the dealer's hand, remove it from the unseen cards, and repeat these steps from the new hand, weighting each rank by its chance of being drawn. Every draw adds at least 1, so every branch ends.

Stopping on a soft 17 makes this a stand-on-soft-17 table. For a table where the dealer hits soft 17, the dealer also draws when $t(d,b) = 17$ and $t(d,b) \neq d$, which is a 17 that counts an ace as 11.

### Row 5. The dealer's finish from the face-up card

Uses: rows 1 to 4.

| Symbol | Meaning |
|---|---|
| $D$ | The dealer's final hand: any value of $x$ from row 4, or BJ (blackjack). BJ beats every total, including a 21 made with three or more cards. |
| $P(D = x \mid u, \mathbf n)$ | The chance the dealer finishes at $x$, given face-up card $u$ and unseen cards $\mathbf n$. The same for $D = \text{BJ}$. |
| $n_{k(u)}$ | Unseen cards of the rank that completes blackjack. Taken as 0 when $k(u)$ is none. |
| $\sum_{r \neq k(u)}$ | Add over every rank except the one that completes blackjack; over all ten when $k(u)$ is none. |
| $b_0$ | The dealer's ace indicator for the two-card hand: 1 if the face-up or face-down card is an ace. |
| $u$, $k(u)$ | Row 3. |
| $x$, $F$, $[\ldots]$, $\max$, $\mathbf n^{-r}$ | Row 4. |
| $r$, $n_r$, $\lvert\mathbf n\rvert$, $P$ | Row 2. |

```math
\begin{aligned}
&P(D=\text{BJ}\mid u,\mathbf n)=\frac{n_{k(u)}}{\lvert\mathbf n\rvert}\\[8pt]
&P(D=x\mid u,\mathbf n)=\sum_{r\ne k(u)}\frac{n_r}{\lvert\mathbf n\rvert}\,F\bigl(x\mid u+r,\ b_0,\ \mathbf n^{-r}\bigr)\\[6pt]
&\text{where } b_0=\max\bigl([u=1],[r=1]\bigr)
\end{aligned}
```

The dealer's first draw is the face-down card. If it is the completing rank, the dealer has blackjack. Any other card gives the dealer a two-card hand with hard total $u + r$, and row 4 takes over from there.

### Row 6. Standing

Uses: rows 1, 3 and 5.

| Symbol | Meaning |
|---|---|
| $P_{\text{win}}$, $P_{\text{lose}}$ | The chance you win, and the chance you lose, if you stand now with hard total $h$ and ace indicator $a$, against face-up card $u$ with unseen cards $\mathbf n$. |
| $V_{\text{stand}}(h,a,u,\mathbf n)$ | Your average result per dollar if you stand now. |
| $\mathbf n$ | Row 2. Here it is the unseen cards at this point, after any cards you have drawn. |
| $h$, $a$, $t$ | Row 1. |
| $u$ | Row 3. |
| $D$, $P(D = \ldots \mid u,\mathbf n)$ | Row 5. |

```math
\begin{aligned}
P_{\text{win}}&=P\bigl(D=\text{bust or } D<t(h,a)\mid u,\mathbf n\bigr)\\
P_{\text{lose}}&=P\bigl(D>t(h,a)\text{ or } D=\text{BJ}\mid u,\mathbf n\bigr)\\[6pt]
V_{\text{stand}}(h,a,u,\mathbf n)&=P_{\text{win}}-P_{\text{lose}}
\end{aligned}
```

Standing fixes your total, so the result depends only on where the dealer finishes. You win a dollar if the dealer busts or stops below you, and lose a dollar if the dealer stops above you or has blackjack. A tie pays 0, so it drops out of the average. Its chance, $1 - P_{\text{win}} - P_{\text{lose}}$, is used in row 11.

### Row 7. Playing on: hit or stand

Uses: rows 1, 2, 3, 4 and 6.

| Symbol | Meaning |
|---|---|
| $V(h,a,u,\mathbf n)$ | Your average result per dollar from this point, choosing hit or stand as well as possible at every step from here. |
| $V_{\text{hit}}(h,a,u,\mathbf n)$ | The same, if you take one card now and then choose as well as possible. |
| $a'$ | Your ace indicator after drawing rank $r$: 1 if you already had an ace or the new card is one. |
| $h$, $a$ | Row 1. |
| $r$, $n_r$, $\lvert\mathbf n\rvert$, $\sum$ | Row 2. |
| $u$ | Row 3. |
| $[\ldots]$, $\max$, $\mathbf n^{-r}$ | Row 4. |
| $V_{\text{stand}}$ | Row 6. |

```math
\begin{aligned}
&\text{If } h>21: && V(h,a,u,\mathbf n)=-1\\[6pt]
&\text{If } h\le 21: && V(h,a,u,\mathbf n)=\max\bigl(V_{\text{stand}}(h,a,u,\mathbf n),\ V_{\text{hit}}(h,a,u,\mathbf n)\bigr)\\[10pt]
& && V_{\text{hit}}(h,a,u,\mathbf n)=\sum_{r=1}^{10}\frac{n_r}{\lvert\mathbf n\rvert}\,V\bigl(h+r,\ a',\ u,\ \mathbf n^{-r}\bigr)\\[6pt]
&\text{where} && a'=\max\bigl(a,[r=1]\bigr)
\end{aligned}
```

$V$ and $V_{\text{hit}}$ are defined together because each uses the other. Past 21 you have bust and lost your bet. Otherwise you take whichever of standing and hitting has the higher average. That comparison is what "as well as possible" means here, and it is the only place the model makes a decision for you. Hitting is valued by going through every rank you could draw: add it to your hand, remove it from the unseen cards, and value the new position with the first equation, weighting each rank by its chance. Every hit adds at least 1 to your hard total, so every branch ends in a stand or a bust.

### Row 8. Value of an opening

Uses: rows 2, 3, 4, 5 and 7.

| Symbol | Meaning |
|---|---|
| $c_1$, $c_2$ | The ranks of your first and second cards. |
| $\mathbf n'$ | The unseen cards once your two cards and the dealer's face-up card are dealt: one card removed for each of $c_1$, $c_2$ and $u$, also written $\mathbf n^{-c_1-c_2-u}$. If two of them share a rank, two come off that count. |
| $a_0$ | Your ace indicator for the two-card hand: 1 if either card is an ace. |
| $v(c_1,c_2,u,\mathbf n)$ | Your average result per dollar from this opening. |
| $\lvert\cdot\rvert$ | Row 2. |
| $u$, $k(u)$ | Row 3. |
| $[\ldots]$, $\max$, $\mathbf n^{-r}$ | Row 4. |
| $n_{k(u)}$ | Row 5, here counted in $\mathbf n'$. |
| $V$ | Row 7. |

```math
\begin{aligned}
&\text{If your cards are an ace and a ten-value card, } \{c_1,c_2\}=\{1,10\}:\\
&\qquad v(c_1,c_2,u,\mathbf n)=1.5\left(1-\frac{n'_{k(u)}}{\lvert\mathbf n'\rvert}\right)\\[10pt]
&\text{Otherwise:}\\
&\qquad v(c_1,c_2,u,\mathbf n)=V\bigl(c_1+c_2,\ a_0,\ u,\ \mathbf n'\bigr)\\[6pt]
&\text{where } a_0=\max\bigl([c_1=1],[c_2=1]\bigr)
\end{aligned}
```

With an ace and a ten-value card you have blackjack, paid 1.5 times your bet unless the dealer also has blackjack, which is a tie worth 0. Without blackjack the hand plays on from hard total $c_1 + c_2$, and a possible dealer blackjack is already one of the dealer's finishes in rows 5 and 6.

Many tables have the dealer check the face-down card for blackjack before you play. With hit and stand as your only options, that check doesn't change the answer: a dealer blackjack costs you exactly your bet whether it is revealed early or late, and no choice you make changes that amount.

### Row 9. Chance of an opening

Uses: rows 2, 3, 4 and 8.

| Symbol | Meaning |
|---|---|
| $P(c_1,c_2,u \mid \mathbf n)$ | The chance you are dealt $c_1$ then $c_2$ and the dealer shows $u$, given unseen cards $\mathbf n$. |
| $n_{c_1}$, $n_{c_2}$, $n_u$ | The count $n_r$ for rank $c_1$, $c_2$ or $u$. |
| $\lvert\mathbf n\rvert$ | Row 2. |
| $u$ | Row 3. |
| $[\ldots]$ | Row 4. Here $[c_2 = c_1]$ is 1 when your two cards share a rank. |
| $c_1$, $c_2$ | Row 8. |

```math
P(c_1,c_2,u\mid\mathbf n)=
\frac{n_{c_1}}{\lvert\mathbf n\rvert}\cdot
\frac{n_{c_2}-[c_2=c_1]}{\lvert\mathbf n\rvert-1}\cdot
\frac{n_u-[u=c_1]-[u=c_2]}{\lvert\mathbf n\rvert-2}
```

The first card is rank $c_1$ with chance equal to the unseen cards of that rank over all unseen cards. For the second, one card is gone from the total, and one from rank $c_1$'s count if the ranks match. For the third, two are gone from the total, and one from its rank's count for each earlier card it matches. Two aces for you and a dealer ace from a fresh deck: $\frac{4}{52}\cdot\frac{3}{51}\cdot\frac{2}{50}$.

Your first and second cards are counted in order, so a ten then a 3 and a 3 then a ten are separate openings. The real dealing order (your card, the dealer's card, your card) gives the same answer, because every order of draws from shuffled cards is equally likely.

### Row 10. Expected return

Uses: rows 2, 3, 8 and 9.

| Symbol | Meaning |
|---|---|
| $E(\mathbf n)$ | Your average result per dollar bet on the next round. Positive means the remaining cards favour you. |
| $\mathbf n$, $\sum$ | Row 2. |
| $u$ | Row 3. |
| $c_1$, $c_2$, $v$ | Row 8. |
| $P(c_1,c_2,u\mid\mathbf n)$ | Row 9. |

```math
E(\mathbf n)=\sum_{c_1=1}^{10}\ \sum_{c_2=1}^{10}\ \sum_{u=1}^{10} P(c_1,c_2,u\mid\mathbf n)\; v(c_1,c_2,u,\mathbf n)
```

Go through all 1,000 openings (10 first cards × 10 second cards × 10 dealer face-up cards), multiply each one's chance by what it is worth with best play, and add the products. An opening that needs more cards of a rank than remain gets chance 0 from row 9. You compute this before the deal, from the unseen cards the previous round left, and it is the number that decides the bet.

### Row 11. Chance of each result

Uses: rows 1 to 4 and 6 to 9.

Rows 1 to 10 produce averages. The bet-size rule in row 13 also needs to know how widely single rounds vary, which needs the chance of each result. This row follows the same branches as rows 7 and 8, making the choice row 7 made at each point and carrying chances instead of averages.

| Symbol | Meaning |
|---|---|
| $W$ | One round's result per dollar: 1.5, 1, 0 or −1. |
| $w$ | One of those four values. |
| $Q(w \mid h,a,u,\mathbf n)$ | The chance the round ends at result $w$, starting from your hand with hard total $h$ and ace indicator $a$, and playing as row 7 chooses. When standing and hitting have equal averages, standing is taken; either gives the same average. |
| $q(w)$ | The chance the round ends at result $w$ from a given opening $c_1$, $c_2$, $u$. |
| $P(W = w)$ | The chance the next round ends at result $w$. |
| $h$, $a$ | Row 1. |
| $r$, $n_r$, $\lvert\mathbf n\rvert$, $\sum$ | Row 2. |
| $u$, $k(u)$ | Row 3. |
| $[\ldots]$, $\mathbf n^{-r}$ | Row 4. |
| $P_{\text{win}}$, $P_{\text{lose}}$, $V_{\text{stand}}$ | Row 6. |
| $V_{\text{hit}}$, $a'$ | Row 7. |
| $c_1$, $c_2$, $\mathbf n'$, $a_0$ | Row 8. |
| $P(c_1,c_2,u\mid\mathbf n)$ | Row 9. |

From a position in your hand:

```math
\begin{aligned}
&\text{If } h>21: && Q(w\mid h,a,u,\mathbf n)=[w=-1]\\[6pt]
&\text{If } h\le 21 \text{ and } V_{\text{stand}}\ge V_{\text{hit}}: && Q(1\mid\ldots)=P_{\text{win}},\quad Q(0\mid\ldots)=1-P_{\text{win}}-P_{\text{lose}},\quad Q(-1\mid\ldots)=P_{\text{lose}},\quad Q(1.5\mid\ldots)=0\\[6pt]
&\text{If } h\le 21 \text{ and } V_{\text{stand}}<V_{\text{hit}}: && Q(w\mid h,a,u,\mathbf n)=\sum_{r=1}^{10}\frac{n_r}{\lvert\mathbf n\rvert}\,Q\bigl(w\mid h+r,\ a',\ u,\ \mathbf n^{-r}\bigr)
\end{aligned}
```

From an opening, then over all openings:

```math
\begin{aligned}
&\text{If your cards are an ace and a ten-value card:} && q(1.5)=1-\frac{n'_{k(u)}}{\lvert\mathbf n'\rvert},\quad q(0)=\frac{n'_{k(u)}}{\lvert\mathbf n'\rvert},\quad q(1)=q(-1)=0\\[8pt]
&\text{Otherwise:} && q(w)=Q\bigl(w\mid c_1+c_2,\ a_0,\ u,\ \mathbf n'\bigr)\\[10pt]
& && P(W=w)=\sum_{c_1=1}^{10}\ \sum_{c_2=1}^{10}\ \sum_{u=1}^{10} P(c_1,c_2,u\mid\mathbf n)\; q(w)
\end{aligned}
```

A bust is a certain loss. A stand ends the round with the three chances from row 6. A hit spreads the chance over every card you could draw. Two checks on an implementation: the four values of $P(W = w)$ add to 1, and $1.5 \times P(W=1.5) + P(W=1) - P(W=-1)$ equals $E(\mathbf n)$ from row 10.

### Row 12. Variance

Uses: rows 10 and 11.

| Symbol | Meaning |
|---|---|
| $\sigma^2$ | The variance of $W$: the average squared distance between a round's result and $E(\mathbf n)$. |
| $W$, $w$, $P(W = w)$ | Row 11. |
| $E(\mathbf n)$ | Row 10. |

```math
\sigma^2=\sum_{w\in\{1.5,\ 1,\ 0,\ -1\}} P(W=w)\,\bigl(w-E(\mathbf n)\bigr)^2
```

Variance measures how far single rounds land from the average. Nearly every round in this game is a full win or a full loss, so it comes out a little under 1: 0.974 for a fresh single deck.

### Row 13. Bet size

Uses: rows 10 and 12.

| Symbol | Meaning |
|---|---|
| $f^\ast$ | The fraction of your bankroll to bet. |
| $B$ | Your bankroll: the money set aside for this play. |
| $E(\mathbf n)$ | Row 10. |
| $\sigma^2$ | Row 12. |

```math
\begin{aligned}
&f^\ast=\frac{E(\mathbf n)}{\sigma^2}\\[8pt]
&\text{If } E(\mathbf n)>0: && \text{bet}=f^\ast B\\
&\text{If } E(\mathbf n)\le 0: && \text{bet}=\text{table minimum}
\end{aligned}
```

This is the Kelly criterion, the bet-sizing rule that makes a bankroll grow fastest over many rounds. Its exact form picks the fraction $f$ that maximises the average of $\log(1 + fW)$. Dividing the edge by the variance approximates that answer when the edge is a few percent or less, which covers nearly every round. Near the end of a shoe, unusual remaining cards can produce larger edges, and there the exact form should be used. For example, an edge of 0.3% with variance near 1 means betting about 0.3% of the bankroll. At zero or negative edge the rule says bet nothing, which the table doesn't allow, so you bet the minimum or sit the round out. The table maximum caps the bet from above.

---

## Worked example: one round from a fresh single deck

Setting: one fresh 52-card deck, $\mathbf n = (4,4,4,4,4,4,4,4,4,16)$, and the rules above.

### Before the deal

Row 10 needs all 1,000 openings. The table follows one of them, a 10 then a 6 for you against a dealer 10, through rows 1 to 9, then gives the results of rows 10 to 13, which cover every opening.

| Row | Inputs | Output | Meaning |
|---|---|---|---|
| 1 | Your cards 10 and 6 | $h = 16$, $a = 0$, $t = 16$ | You hold a hard 16. |
| 2 | Fresh deck, then the three opening cards removed | 52 unseen before the deal; 49 after, with $\mathbf n' = (4,4,4,4,4,3,4,4,4,14)$ | Of the 49 cards left, 14 are worth 10. |
| 3 | $u = 10$ | $k(u) = 1$ | An ace under the dealer's 10 would be a dealer blackjack. |
| 4 | Suppose the face-down card is a 6: dealer hard 16, 48 unseen | 17, 18, 19, 20 and 21: 8.3% each; bust: 58.3% | A dealer forced to draw on 16 busts on any 6 or higher, 28 of the 48 unseen cards. |
| 5 | $u = 10$, $\mathbf n'$ | BJ 8.2%, 17: 11.9%, 18: 11.6%, 19: 11.8%, 20: 32.0%, 21: 3.6%, bust 21.0% | Against a dealer 10, 20 is the most likely finish, and the dealer busts about one round in five. |
| 6 | $h = 16$, $a = 0$, $u = 10$, $\mathbf n'$ | $P_{\text{win}} = 0.210$, $P_{\text{lose}} = 0.790$, $V_{\text{stand}} = -0.580$ | Standing on 16 here loses 58 cents per dollar on average, because you win only when the dealer busts. |
| 7 | Same | $V_{\text{hit}} = -0.547$, so $V = -0.547$ | Hitting loses about 55 cents per dollar, 3 cents less than standing, so best play hits. |
| 8 | $c_1 = 10$, $c_2 = 6$, $u = 10$ | $v = -0.547$ | Not a blackjack, so the opening is worth best play from hard 16. |
| 9 | Same, fresh deck | $\frac{16}{52}\cdot\frac{4}{51}\cdot\frac{15}{50} = 0.0072$ | About 1 round in 138 opens this way. |
| 10 | Rows 8 and 9 for all 1,000 openings | $E(\mathbf n) = -0.0193$ | From a fresh deck you lose about 1.9 cents per dollar bet. |
| 11 | Same branches | Win 39.0%, loss 47.9%, push 8.4%, your blackjack paid 4.6% | You lose more rounds than you win, and the extra half-dollar on your blackjacks narrows the gap without closing it: $0.390 + 1.5 \times 0.0465 - 0.479 = -0.0193$. |
| 12 | Row 11 and $E(\mathbf n)$ | $\sigma^2 = 0.974$ | A single round typically lands about a dollar away from the average. |
| 13 | $E(\mathbf n)$, $\sigma^2$, $B$ | $f^\ast = -0.0198$, so bet the table minimum | The remaining cards are against you, so you bet the minimum or sit out. |

Split by the dealer's face-up card, row 10's total of −0.0193 is built from these averages:

| Dealer shows | Chance | Your average result per dollar |
|---|---|---|
| Ace | 7.7% | −0.363 |
| 2 | 7.7% | +0.074 |
| 3 | 7.7% | +0.105 |
| 4 | 7.7% | +0.143 |
| 5 | 7.7% | +0.185 |
| 6 | 7.7% | +0.190 |
| 7 | 7.7% | +0.124 |
| 8 | 7.7% | +0.042 |
| 9 | 7.7% | −0.051 |
| 10 | 30.8% | −0.175 |

A ten-value face-up card is four times as common as any other rank and costs you 17.5 cents per dollar. Low face-up cards favour you but don't come often enough to make up for the ace and the ten.

### During the round

The cards come out: you get 10 and 6, the dealer shows 10. Your bet is fixed, so rows 6 and 7 are the only rows that still matter.

1. Hard 16 against a 10: standing averages −0.580 and hitting −0.547, so you hit.
2. You draw a 3. Remove it from the unseen cards; your hard total is 19. Standing now averages −0.004 and hitting −0.728, so you stand. 19 against a dealer 10 is close to break-even, because the dealer finishes on 20 about a third of the time.
3. You have no decisions left. The dealer turns over the face-down card and draws by the row 4 rule, and the bet is settled.

### After the round

Every card seen in the round (your 10, 6 and 3, the dealer's face-up card, face-down card and any cards the dealer drew) comes out of $\mathbf n$. That smaller $\mathbf n$ goes into row 10 to choose the next bet.

---

## How the calculation is run

The rows nest. Row 10 runs rows 8 and 9 for each of the 1,000 openings. Inside each opening, row 7 branches over all 10 ranks for every card you might draw. At every point where you could stand, row 6 calls row 5, which branches over the dealer's face-down card, and row 4 branches again over each card the dealer draws.

Many branches reach the same position: the same hard total, ace indicator, face-up card and unseen counts, reached through a different order of cards. A program stores each position's value the first time it computes it and reuses the stored value after that, which keeps the run short. In a Python implementation we tested, a fresh single deck took about 4 seconds and a fresh six-deck shoe about 11 seconds. The six-deck shoe gives $E(\mathbf n) = -0.0234$.

## What the model leaves out

- Doubling, splitting, insurance and surrender. Adding an option can only raise $E(\mathbf n)$, because best play is free to ignore it. In a game that allows them, the true average is therefore at least the model's $E(\mathbf n)$, and remaining cards the model scores slightly negative can be favourable in that game.
- Tables where the dealer hits soft 17 need the change described under row 4.
- The model needs an exact count of every rank seen. Card counters at a real table usually keep a single running score instead, which follows $E(\mathbf n)$ only roughly.
- Cards other players receive before your turn come out of $\mathbf n$ before your hit-or-stand decisions, but $E(\mathbf n)$ itself is computed as if you were alone.
- The model assumes the unseen cards are in random order. With a continuous shuffling machine, played cards go back into the machine after each round, so they never leave $\mathbf n$ and $E(\mathbf n)$ stays at its fresh-shoe value every round.
