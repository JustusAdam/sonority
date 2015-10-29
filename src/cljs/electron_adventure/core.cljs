(ns electron-adventure.core
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [electron-adventure.fileview :as fv]))

(def counter (reagent/atom 0))


(defn counter-box []
  [:div
    (str "Counter with value" @counter)
    [:div
      [:button {:on-click #(swap! counter inc)} "Up"]
      [:button {:on-click #(swap! counter dec)} "Down"]
      ]])

(defn main-page
  []
  [:div "Hello World!"
    (counter-box)
    (fv/fileview)])

(defn mount-root
  []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (mount-root))
