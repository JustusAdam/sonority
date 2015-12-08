(ns sonority.alert
  (:require [reagent.core :as reagent]
            [cljs.nodejs :as nodejs]))


(defrecord Alert [message choices])
(defrecord Choice [value handler])

(nodejs/enable-util-print!)

(defonce alert-queue (reagent/atom []))


(defn hide-alert []
  (swap! alert-queue pop))

(defn yes-no-alert [message on-yes on-no]
  (->Alert message
           [(->Choice "Yes" #(do hide-alert on-yes))
            (->Choice "No" #(do hide-alert on-no))]))

(defn return-value-alert [message choices handler]
  (->Alert message
           (map (fn [choice] (->Choice choice #(handler choice))) choices)))

(defn- confirm-alert-internal [message on-confirm]
  (->Alert message
           [(->Choice "Confirm" on-confirm)]))


(defn confirm-alert
  ([message] (confirm-alert-internal message hide-alert))
  ([message handler] (confirm-alert-internal message #(do hide-alert handler))))


(defn- alert-internal
  [message choices])

(defn show-alert [alert]
  (swap! alert-queue #(conj % alert)))


(defn alert-display []
  (let [[alert & _ ] @alert-queue]
    (if-not (nil? alert)
      [:div.alert.backdrop.row
       [:div.alert.inner.small-offset-2.small-8.column
        [:div.row
         [:div.column.small-12 (:message alert)]]
           (doall
           (for [{value :value handler :handler} (:choices alert)]
             ^{:key value}
             [:div.column.small-3
              [:button {:on-click handler} value]]))]])))
