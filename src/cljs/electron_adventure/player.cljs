(ns electron-adventure.player
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]))

(defrecord File [name path])

(def selected-piece (reagent/atom nil))

(def should-play (reagent/atom false))

(def current-duration (reagent/atom 0))

(def player-ref (reagent/atom nil))

(defn toggle-player [key ref old-state new-state]
  (let [player (.getElementById js/document "main-player")]
    (if new-state
      (.play player)
      (.pause player))
    true))

(add-watch should-play :player-link toggle-player)

(defn select-new [piece]
  (do
    ;(reset! should-play true)
    (reset! selected-piece piece)))


(defn controls []
  [:div.controls
    [:button {:on-click #(swap! should-play not)} (if @should-play "||" ">")]
    [:span @current-duration]])


(defn player [piece]
  [:audio#main-player {:src (:path @selected-piece) :controls true :autoPlay @should-play} "No support?"])

(defn interface [piece]
  [:div
    [:div [:p "Playing: " (if (empty? (:name piece)) "nothing" (:name piece) )]]
    (player piece)
    (controls)])

(defn std-interface []
  (interface @selected-piece))
