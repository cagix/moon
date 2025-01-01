(ns cdq.game-loop
  (:require [anvil.controls :as controls]
            [cdq.context :refer [line-of-sight? render-z-order active-entities
                                 check-player-input
                                 update-mouseover-entity
                                 update-paused-state
                                 update-time
                                 tick-potential-fields
                                 tick-entities
                                 remove-destroyed-entities
                                 check-camera-controls
                                 check-ui-key-listeners]]
            [cdq.debug :as debug]
            [cdq.tile-color-setter :as tile-color-setter]
            [clojure.component :as component]
            [clojure.gdx :refer [clear-screen black]]
            [clojure.utils :refer [pretty-pst sort-by-order]]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui]))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [c entity color]
  (let [[x y] (:left-bottom entity)]
    (c/rectangle c x y (:width entity) (:height entity) color)))

(defn- render-entity! [c system entity]
  (try
   (when show-body-bounds
     (draw-body-rect c entity (if (:collides? entity) :white :gray)))
   (run! #(system % entity c) entity)
   (catch Throwable t
     (draw-body-rect c entity :red)
     (pretty-pst t))))

(defn- render-entities [{:keys [cdq.context/player-eid] :as c}]
  (let [entities (map deref (active-entities c))
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [component/render-below
                    component/render-default
                    component/render-above
                    component/render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? c player entity))]
      (render-entity! c system entity))))

(def ^:private ^:dbg-flag pausing? true)

(defn render [{:keys [gdl.context/world-viewport
                      cdq.context/tiled-map
                      cdq.context/player-eid
                      cdq.context/raycaster
                      cdq.context/explored-tile-corners]
               :as c}]
  (clear-screen black)
  ; FIXME position DRY
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  ; FIXME position DRY
  (c/draw-tiled-map c
                    tiled-map
                    (tile-color-setter/create raycaster
                                              explored-tile-corners
                                              (cam/position (:camera world-viewport))))
  (c/draw-on-world-view c
                        (fn [c]
                          (debug/render-before-entities c)
                          ; FIXME position DRY (from player)
                          (render-entities c)
                          (debug/render-after-entities c)))
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
    (check-camera-controls c (:camera world-viewport))
    (check-ui-key-listeners c
                            {:controls/close-windows-key controls/close-windows-key
                             :controls/window-hotkeys    controls/window-hotkeys}
                            (c/stage c))
    c))
