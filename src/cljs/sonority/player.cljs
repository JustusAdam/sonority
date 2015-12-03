(ns sonority.player
  (:require [reagent.core :as reagent]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [sonority.util :as util]
            [clojure.zip :refer [vector-zip]]))


(nodejs/enable-util-print!)



; independant utility functions


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

(defonce volume (reagent/atom 0.5))

(def volume-alter-interval 0.05)



(defn alter-volume
  "Increase or decrease the volume"
  [amount]
  (swap! volume
    (fn [vol]
      (let [new-vol (+ amount vol)]
        (cond
          (< 1 new-vol) 1.0
          (> 0 new-vol) 0
          :else new-vol)))))

(defn- player-volume-updater [key ref old-state new-state]
  (set! (.-volume @player) new-state))

(defn- update-player-time [] (reset! current-player-time (.-currentTime @player)))

(defn- player-time-updater [key ref old-state new-state]
  (update-player-time))

(defn- attach-clock-player-time-updater []
  (add-watch clock :player-time-updater player-time-updater))

(defn- detach-clock-player-time-updater []
  (remove-watch clock :player-time-updater))

(defn select-new
  "Set the selected piece."
  [piece]
  (do
    (reset! selected-piece piece)))

(defn play-next
  "Pop the topmost element of the queue and play it in the player."
  []
  (swap! queue (fn [[x & r]] (do (select-new x) r))))

(defn- create-player [piece]
  (let [elem (if (nil? piece) (js/Audio.) (js/Audio. piece))]
    (do
      (.addEventListener elem "durationchange" #(update-player-time))
      (.addEventListener elem "ended" #(play-next))
      (set! (.-volume elem) @volume)
      (if @playing (.play elem))
      elem)))

(defn- toggle-player [key ref old-state new-state]
  (if new-state
    (do
      (.play @player))
    (do
      (.pause @player))))

(defn- toggle-piece [key ref old-state new-state]
  (if (not= old-state new-state)
    (do
      (set! (.-src @player) (:path new-state))
      (if @playing
        (.play @player)))))

(defn add-as-next
  "Push the element as the topmost element onto the queue."
  [file]
  (swap! queue #(cons % file)))

(defn- rem-q-item-where [pred]
  (swap! queue (fn [queue] (util/drop-where pred queue))))

(defn add-to-queue
  "Append the element as last element in the queue."
  [file]
  (swap! queue #(conj % file)))

(defn remove-queue-item [a]
  (cond
    (integer? a) (swap! queue #(util/drop-nth a %))
    (fn? a) (rem-q-item-where a)
    :else (rem-q-item-where #(= a %))))


; ui

(defn controls
  "Collection of elements used to interact with the player"
  []
  (let [player-time (.-duration @player)
        current @current-player-time]
    [:div.controls
      [:div (:title @selected-piece)]
      [:button {:on-click #(swap! playing not)} (if @playing "||" ">")]
      [:progress.progress
        { :value (if (js/isNaN current) 0 current)
          :max (if (js/isNaN player-time) 0 player-time)}]
      [:span (str (format-time current) "/" (format-time player-time))]
      [:button {:on-click #(play-next)} "next"]
      [:button {:on-click #(alter-volume volume-alter-interval)} "Volume up"]
      [:button {:on-click #(alter-volume (- 0 volume-alter-interval))} "Volume down"]
      [:progress.progress {:value @volume}]]))

(defn std-interface
  "A UI for the player."
  []
  [:div
    [:section
      [:h3 "Queue"]
      [:table
        (doall
          (for [[index file] (map vector (range) @queue)]
            ^{:key (str "queue-" index (:name file))}
            [:tr
              [:td (str (:title file))]
              [:td [:a {:on-click #(remove-queue-item (int index))} "remove"]]]))]]
    (controls)])


; wiring watchers

(reset! player (create-player nil))

(defonce clock-updater
  (js/setInterval #(reset! clock (js/Date.)) 1000))

(add-watch playing :player-link toggle-player)

(add-watch selected-piece :piece-link toggle-piece)

(add-watch volume :volume-updater player-volume-updater)

(attach-clock-player-time-updater)
