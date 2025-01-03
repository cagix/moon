(ns cdq.render
  (:require [cdq.context :refer [active-entities render-z-order line-of-sight? draw-body-rect]]
            [gdl.error :refer [pretty-pst]]
            [gdl.utils :refer [defsystem sort-by-order]]))

(defsystem render-below)
(defmethod render-below :default [_ entity c])

(defsystem render-default)
(defmethod render-default :default [_ entity c])

(defsystem render-above)
(defmethod render-above :default [_ entity c])

(defsystem render-info)
(defmethod render-info :default [_ entity c])

(def ^:private ^:dbg-flag show-body-bounds false)

(defn entities
  "Draws all active entities, sorted by the `:z-order` and with the render systems `below`, `default`, `above`, `info` for each z-order if the entity is in line-of-sight? to the player entity or is an `:z-order/effect`.

  Optionally for debug purposes body rectangles can be drown which show white for collidings and gray for non colliding entities.

  If an error is thrown during rendering, the entity body drawn with a red rectangle and the error is pretty printed to the console."
  [{:keys [cdq.context/player-eid] :as c}]
  (let [entities (map deref (active-entities c))
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [render-below
                    render-default
                    render-above
                    render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? c player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect c entity (if (:collides? entity) :white :gray)))
       (run! #(system % entity c) entity)
       (catch Throwable t
         (draw-body-rect c entity :red)
         (pretty-pst t))))))
