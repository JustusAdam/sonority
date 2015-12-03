(ns audio.types)


(defn get-meta-val
  ([obj key val] (get (:meta obj) key val))
  ([obj key]  (let [v (get (:meta obj) key)]
                (case v
                  nil (case key
                        :artist "Unknown Artist"
                        :album "Unknown Album"
                        :tracknumber 0
                        nil)
                  v))))

(defprotocol AlbumAssociated
  (album-identifier [x]))

(defrecord Album [title meta tracks]
  AlbumAssociated
  (album-identifier [x]
    (hash (str (:title x) "-" (get-meta-val x :artist) "-" (get-meta-val x :year)))))


(defrecord Track [title meta path]
  AlbumAssociated
  (album-identifier [x]
    (hash (str (get-meta-val x :album) "-" (get-meta-val x :artist) "-" (get-meta-val x :year)))))


(defn meta-track-to-album [meta]
  (let [albumtitle (:album meta)]
    (assoc
      (dissoc meta :tracknumber :title :track)
      :title albumtitle)))
