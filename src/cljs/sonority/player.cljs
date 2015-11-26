(ns sonority.player
  (:require [reagent.core :as reagent]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [sonority.util :as util]
            [clojure.zip :refer [vector-zip]]))


(nodejs/enable-util-print!)



; independant utility functions

(defrecord File [name path])


(defn format-time [c]
  (let [d (int c)]
    (if (js/isNaN c)
      "-:-"
      (let [h (int (/ d 60))
            min (int (mod d 60))]
        (str h ":" (if (< min 10) "0" "") min)))))



(defonce selected-piece (reagent/atom nil))

(defonce playing (reagent/atom false))

(defonce current-player-time (reagent/atom 0))

(defonce clock (reagent/atom (js/Date.)))

(defonce player (reagent/atom nil))

(defonce queue (reagent/atom []))



(defn- update-player-time [] (reset! current-player-time (.-currentTime @player)))

(defn- player-time-updater [key ref old-state new-state]
  (update-player-time))

(defn- attach-clock-player-time-updater []
  (add-watch clock :player-time-updater player-time-updater))

(defn- detach-clock-player-time-updater []
  (remove-watch clock :player-time-updater))

(defn- create-player [piece]
  (let [elem (if (nil? piece) (js/Audio.) (js/Audio. piece))]
    (do
      (.addEventListener elem "player-timechange" #(update-player-time))
      (if @playing (.play elem))
      elem)))

(defn- toggle-player [key ref old-state new-state]
  (if new-state
    (do
      (attach-clock-player-time-updater)
      (.play @player))
    (do
      (detach-clock-player-time-updater)
      (.pause @player))))

(defn toggle-piece [key ref old-state new-state]
  (if (not= old-state new-state)
    ; (reset! player (create-player (:path new-state)))))
    (swap! player #(do (.pause %) (create-player (:path new-state))))))

(defn select-new [piece]
  (do
    ;(reset! playing true)
    (reset! selected-piece piece)))

(defn play-next []
  (swap! queue (fn [[x & r]] (do (reset! selected-piece x) r))))

(defn add-as-next [file]
  (swap! queue #(cons % file)))

(defn- rem-q-item-where [pred]
  (swap! queue (fn [queue] (util/drop-where pred queue))))

(defn add-to-queue [file]
  (swap! queue #(conj % file)))

(defn remove-queue-item [a]
  (cond
    (integer? a) (swap! queue #(util/drop-nth a %))
    (fn? a) (rem-q-item-where a)
    :else (rem-q-item-where #(= a %))))


; ui

(defn controls []
  (let [player-time (.-duration @player)
        current @current-player-time]
    [:div.controls
      [:button {:on-click #(swap! playing not)} (if @playing "||" ">")]
      [:progress.progress
        { :value (let [val (/ (if (js/isNaN current) 0 current) player-time)]
                   (if (js/isNaN val) 0 val))}]
      [:span (str (format-time current) "/" (format-time player-time))]
      [:button {:on-click #(play-next)} "next"]]))

(defn interface [piece]
  [:div
    [:div [:p "Playing: " (if (empty? (:name piece)) "nothing" (:name piece))]]
    [:table
      (doall
        (for [[index file] (vector-zip [(range) @queue])]
          ^{:key (str "queue-" index (:name file))}
            [:tr
              [:td (str (:name file))]
              [:td [:a {:on-click #(remove-queue-item index)} "remove"]]]))]
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
