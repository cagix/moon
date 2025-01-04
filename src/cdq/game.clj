(ns cdq.game
  (:require [clojure.gdx :refer [clear-screen black]]
            [gdl.context :as c]
            [gdl.error :refer [pretty-pst]]
            [gdl.ui :as ui]
            [gdl.utils :refer [sort-by-order]]
            [cdq.context :refer [active-entities render-z-order line-of-sight? draw-body-rect
                                 set-camera-on-player-position
                                 render-tiled-map
                                 render-before-entities
                                 render-after-entities
                                 check-player-input
                                 update-mouseover-entity
                                 update-paused-state
                                 update-time
                                 tick-potential-fields
                                 tick-entities
                                 remove-destroyed-entities
                                 check-ui-key-listeners]]
            [cdq.entity.render :as render]))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- render-entities
  "Draws all active entities, sorted by the `:z-order` and with the render systems `below`, `default`, `above`, `info` for each z-order if the entity is in line-of-sight? to the player entity or is an `:z-order/effect`.

  Optionally for debug purposes body rectangles can be drown which show white for collidings and gray for non colliding entities.

  If an error is thrown during rendering, the entity body drawn with a red rectangle and the error is pretty printed to the console."
  [{:keys [cdq.context/player-eid] :as c}]
  (let [entities (map deref (active-entities c))
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [render/below
                    render/default
                    render/above
                    render/info]
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

(def ^:private ^:dbg-flag pausing? true)

(defn process-frame [c]
  (clear-screen black)
  ; FIXME position DRY
  (set-camera-on-player-position c)
  ; FIXME position DRY
  (render-tiled-map c)
  ; render/entities
  (c/draw-on-world-view c
                        (fn [c]
                          (render-before-entities c)
                          ; FIXME position DRY (from player)
                          (render-entities c)
                          (render-after-entities c)))
  (let [stage (c/stage c)]
    (ui/draw stage c)
    (ui/act  stage c))
  (check-player-input c)
  (let [c (-> c
              update-mouseover-entity
              (update-paused-state pausing?))
        c (if (:cdq.context/paused? c)
            c
            (-> c
                update-time
                tick-potential-fields
                tick-entities))]
    (remove-destroyed-entities c) ; do not pause this as for example pickup item, should be destroyed.
    (c/check-camera-controls c)
    (check-ui-key-listeners c)
    c))
