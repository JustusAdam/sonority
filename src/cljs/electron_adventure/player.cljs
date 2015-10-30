(ns electron-adventure.player
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]))

(defrecord File [name path])

(def selected-piece (reagent/atom nil))

(def should-play (reagent/atom false))

(defn toggle-player [key ref old-state new-state]
  (let [player (.getElementById js/document "main-player")]
    (do
      (println new-state)
      (if new-state
        (.play player)
        (.pause player))
      true)))

(add-watch should-play :player-link toggle-player)

(defn select-new [piece]
  (do
    (reset! should-play true)
    (reset! selected-piece piece)))

(defn player-interface [piece]
  [:div
    [:div [:p "Playing: " (if (empty? (:name piece)) "nothing" (:name piece) )]]
    [:audio#main-player {:src (:path piece) :controls true :autoPlay true} "No support?"]])

(defn player []
  (player-interface @selected-piece))

(defn player-controls []
  [:div.controls
    [:button {:on-click #(swap! should-play not)} (if @should-play "||" ">")]])
