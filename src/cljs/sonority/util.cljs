(ns sonority.util)

(defn drop-nth [n vec]
  (let [[head t] (split-at n vec)
        [b & tail] t]
    (into [] (concat head tail))))

(defn drop-where [pred vec]
  (let [[head [b & tail]] (split-with pred vec)]
    (into [] (concat head tail))))

(defn map-vals [f m]
  (reduce
    (fn [nmap [k v]] (assoc nmap k (f v)))
    {}
    m))

(defn map-keys [f m]
  (reduce
    (fn [nmap [k v]] (assoc nmap (f k) v))
    {}
    m))
