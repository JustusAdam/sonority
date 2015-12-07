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
  (do
    (swap! alert-queue #(conj % alert))
    (print @alert-queue)))


(defn alert-display []
  (let [[alert & _ ] @alert-queue]
    (if-not (nil? alert)
      [:div.alert.backdrop
       [:div.alert.inner
        [:div.row
         [:div.column.small-12 (:message alert)]
         (doall
           (for [{value :value handler :handler} (:choices alert)]
             [:div.column.small-1
              [:button
               {:on-click handler}
               value]]))]]])))


