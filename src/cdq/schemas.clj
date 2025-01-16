(ns cdq.schemas
 (:require [clojure.edn :as edn]
           [clojure.java.io :as io]
           clojure.assets
           [clojure.db :as db]
           clojure.graphics.animation
           clojure.graphics.sprite))

(defn load-from-edn [schema-edn-file _context]
  (-> schema-edn-file
      io/resource
      slurp
      edn/read-string))

(defmethod clojure.db/edn->value :s/sound [_ sound-name _db {:keys [clojure/assets]}]
  (clojure.assets/sound assets sound-name))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (clojure.graphics.sprite/from-sheet (clojure.graphics.sprite/sheet c
                                                                         file
                                                                         tilew
                                                                         tileh)
                                          [(int (/ sprite-x tilew))
                                           (int (/ sprite-y tileh))]
                                          c))
    (clojure.graphics.sprite/create c file)))

(defmethod clojure.db/edn->value :s/image [_ edn _db c]
  (edn->sprite c edn))

(defmethod clojure.db/edn->value :s/animation [_ {:keys [frames frame-duration looping?]} _db c]
  (clojure.graphics.animation/create (map #(edn->sprite c %) frames)
                                     :frame-duration frame-duration
                                     :looping? looping?))

(defmethod clojure.db/edn->value :s/one-to-one [_ property-id db {:keys [clojure/db] :as context}]
  (db/build db property-id context))

(defmethod clojure.db/edn->value :s/one-to-many [_ property-ids db {:keys [clojure/db] :as context}]
  (set (map #(db/build db % context) property-ids)))
