(ns sonority.player
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]))


(nodejs/enable-util-print!)



; independant utility functions

(defrecord File [name path])




(defonce selected-piece (reagent/atom nil))

(defonce playing (reagent/atom false))

(defonce current-player-time (reagent/atom 0))

(defonce clock (reagent/atom (js/Date.)))

(defonce player (reagent/atom nil))



(defn update-player-time [] (reset! current-player-time (.-currentTime @player)))

(defn player-time-updater [key ref old-state new-state]
  (update-player-time))

(defn attach-clock-player-time-updater []
  (add-watch clock :player-time-updater player-time-updater))

(defn detach-clock-player-time-updater []
  (remove-watch clock :player-time-updater))

(defn create-player [piece]
  (let [elem (if (nil? piece) (js/Audio.) (js/Audio. piece))]
    (do
      (.addEventListener elem "player-timechange" #(update-player-time))
      (if @playing (.play elem))
      elem)))

(defn toggle-player [key ref old-state new-state]
  (if new-state
    (do
      (attach-clock-player-time-updater)
      (.play @player))
    (do
      (detach-clock-player-time-updater)
      (.pause @player))))
  ; (swap! player (fn [player]
  ;   (do
  ;     (if new-state
  ;       (.play player)
  ;       (.pause player))
  ;     player))))

(defn toggle-piece [key ref old-state new-state]
  (if (not= old-state new-state)
    (reset! player (create-player (:path new-state)))))

(defn select-new [piece]
  (do
    ;(reset! playing true)
    (reset! selected-piece piece)))

(defn format-time [c]
  (let [d (int c)]
    (if (js/isNaN c)
      "-:-"
      (let [h (int (/ d 60))
            min (int (mod d 60))]
        (str h ":" (if (< min 10) "0" "") min)))))

; ui

(defn controls []
  (let [player-time (.-duration @player)
        current @current-player-time]
    [:div.controls
      [:button {:on-click #(swap! playing not)} (if @playing "||" ">")]
      [:progress
        { :value (let [val (/ (if (js/isNaN current) 0 current) player-time)]
                   (if (js/isNaN val) 0 val))}]
      [:span (str (format-time current) "/" (format-time player-time))]]))

(defn interface [piece]
  [:div
    [:div [:p "Playing: " (if (empty? (:name piece)) "nothing" (:name piece))]]
    (controls)])

(defn std-interface []
  (interface @selected-piece))


; wiring watchers

(reset! player (create-player nil))

(defonce clock-updater
  (js/setInterval #(reset! clock (js/Date.)) 1000))

(add-watch playing :player-link toggle-player)

(add-watch selected-piece :piece-link toggle-piece)

(attach-clock-player-time-updater)
