(ns sonority.util)

(defn drop-nth [n vec]
  (let [[head [__ tail]] (split-at n vec)]
    (vector (concat head tail))))

(defn drop-where [pred vec]
  (let [[head [__ tail]] (split-with pred vec)]
    (vector (concat head tail))))
