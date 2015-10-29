(ns electron-adventure.player
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]))

(defrecord File [name path])

(def selected-piece (reagent/atom nil))

(defn select-new [piece]
  (reset! selected-piece piece))

(defn player-interface [piece]
  [:div
    [:div [:p "Playing: " (if (empty? (:name piece)) "nothing" (:name piece) )]]
    [:audio {:src (:path piece) :controls true :autoplay "" } "No support?"]])

(defn player []
  (player-interface @selected-piece))
