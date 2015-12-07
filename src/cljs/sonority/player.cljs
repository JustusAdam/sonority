(ns sonority.player
  (:require [reagent.core :as reagent]
            [cljs.nodejs :as nodejs]
            [sonority.util :as util]
            [clojure.zip :refer [vector-zip]]))


(nodejs/enable-util-print!)



; independant utility functions


(defn format-time
  "Format a long representing time to a readable string."
  [c]
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
(def volume-min 0.0)
(def volume-max 1.0)

(def volume-alter-interval 0.05)


(defn alter-volume
  "Increase or decrease the volume by a standard amount."
  [amount]
  (swap! volume
    (fn [vol]
      (let [new-vol (+ amount vol)]
        (cond
          (< volume-max new-vol) 1.0
          (> volume-min new-vol) 0
          :else new-vol)))))

(defn- player-volume-updater
  "Update the volume whenever @volume changes."
  [key ref old-state new-state]
  (set! (.-volume @player) new-state))

(defn- update-player-time [] (reset! current-player-time (.-currentTime @player)))

(defn- player-time-updater [key ref old-state new-state]
  (update-player-time))

(defn- attach-clock-player-time-updater []
  (add-watch clock :player-time-updater player-time-updater))

(defn- detach-clock-player-time-updater []
  (remove-watch clock :player-time-updater))

(defn seek-player
  "Change the players current position."
  [time]
  (swap! player
    (fn [player]
      (do
        (if (and (<= time (.-duration player)) (>= time 0))
          (set! (.-currentTime player) time))
        player))))

(defn select-new
  "Set the selected piece."
  [piece]
  (do
    (reset! selected-piece piece)))

(defn play-next
  "Pop the topmost element of the queue and play it in the player."
  []
  (swap! queue (fn [[x & r]] (do (select-new x) (into [] r)))))

(defn- create-player
  "Create a new player and attach appropriate event handlers to it."
  [piece]
  (let [elem (if (nil? piece) (js/Audio.) (js/Audio. piece))]
    (do
      (.addEventListener elem "durationchange" #(update-player-time))
      (.addEventListener elem "ended" #(play-next))
      (set! (.-volume elem) @volume)
      (if @playing (.play elem))
      elem)))

(defn- toggle-player
  "Toggle the player when @playing changes."
  [key ref old-state new-state]
  (if new-state
    (do
      (.play @player))
    (do
      (.pause @player))))

(defn- toggle-piece
  "Change the currently played piece whenever @selected-piece changes."
  [key ref old-state new-state]
  (if (not= old-state new-state)
    (do
      (set! (.-src @player) (:path new-state))
      (if @playing
        (.play @player)))))

(defn add-as-next
  "Push the element as the topmost element onto the queue."
  [file]
  (swap! queue #(cons % file)))

(defn- rem-q-item-where
  "Remove an item from the queue by predicate."
  [pred]
  (swap! queue (fn [queue] (util/drop-where pred queue))))

(defn add-to-queue
  "Append the element as last element in the queue."
  [file]
  (swap! queue #(conj % file)))

(defn remove-queue-item
  "Remove a queue item."
  [a]
  (cond
    (integer? a) (swap! queue #(util/drop-nth a %))
    (fn? a) (rem-q-item-where a)
    :else (rem-q-item-where #(= a %))))


; ui

(defn slider
  "A html slider with an event handler attached."
  [ target & {:value-scale 1 :min 0 :max 100 :as extras :extra-properties {}}]
  [:input (merge
            { :type "range" :min (:min extras) :max (:max extras)
              :value (* (:value-scale extras) @target)
              :on-change #(reset! target (/ (.-target.value %) (:value-scale extras)))}
            (:extra-properties extras))])

(defn volume-slider
  "Slider for the player volume."
  []
  (slider volume :value-scale 100 :min 0 :max 100))

(defn progress-slider
  "Iteractive slider for the current player position."
  []
  (slider
    current-player-time
    :min 0 :max (* 100 (.-duration @player)) :value-scale 100
    :extra-properties {:style {:width "100%"}
                       :on-change #(seek-player (/ (.-target.value %) 100))}))

(defn controls
  "Collection of elements used to interact with the player"
  []
  (let [player-time (.-duration @player)
        current @current-player-time]
    [:div.controls.row
      [:div.column.small-2
        [:button {:on-click #(swap! playing not)} (if @playing "||" ">")]
        [:button {:on-click #(play-next) :disabled (empty? @queue)} "next"]]
      [:div.column.small-8
        [:div.row
          [:div.column.small-12]
          (:title @selected-piece)]
        [:div.row
          [:div.column.small-12
            (progress-slider)]]]
      [:div.column.small-1
        [:span (str (format-time current) "/" (format-time player-time))]]
      [:div.column.small-1
        (volume-slider)]]))


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
    [:div
      (controls)]])


; wiring watchers

(reset! player (create-player nil))

(defonce clock-updater
  (js/setInterval #(reset! clock (js/Date.)) 1000))

(add-watch playing :player-link toggle-player)

(add-watch selected-piece :piece-link toggle-piece)

(add-watch volume :volume-updater player-volume-updater)

(attach-clock-player-time-updater)
