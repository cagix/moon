(ns cdq.schemas-impl
  (:require [cdq.animation :as animation]
            [cdq.graphics :as g]
            [cdq.val-max :as val-max]
            [gdl.create.db :refer [malli-form]]
            [gdl.schema :as schema]))

(defmethod malli-form :s/val-max [_ _schemas] val-max/schema)

(defn- edn->sprite [{:keys [file sub-image-bounds]} ctx]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (g/sprite-sheet->sprite ctx
                              (g/sprite-sheet ctx file tilew tileh)
                              [(int (/ sprite-x tilew))
                               (int (/ sprite-y tileh))]))
    (g/sprite ctx file)))

(defmethod malli-form :s/sound [_ _schemas] :string)

(defmethod malli-form :s/image [_ _schemas]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod schema/edn->value :s/image [_ edn ctx]
  (edn->sprite edn ctx))

(defmethod malli-form :s/animation [_ _schemas]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]} ctx]
  (animation/create (map #(edn->sprite % ctx) frames)
                    :frame-duration frame-duration
                    :looping? looping?))
