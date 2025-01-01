(ns cdq.game
  (:require [anvil.entity :as entity]
            [cdq.context :refer [line-of-sight? render-z-order active-entities
                                 point->entities
                                 tick-potential-fields
                                 tick-entities
                                 remove-destroyed-entities
                                 check-camera-controls
                                 check-ui-key-listeners]]
            [cdq.debug :as debug]
            [cdq.tile-color-setter :as tile-color-setter]
            [clojure.component :as component :refer [defsystem]]
            [clojure.gdx :as gdx :refer [clear-screen black key-just-pressed? key-pressed?]]
            [clojure.utils :refer [read-edn-resource pretty-pst sort-by-order]]
            [gdl.app :as app]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui]))

(defn- update-time [c]
  (let [delta-ms (min (gdx/delta-time c) max-delta-time)]
    (-> c
        (update :cdq.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defmethod pause-game? :active-skill          [_] false)
(defmethod pause-game? :stunned               [_] false)
(defmethod pause-game? :player-moving         [_] false)
(defmethod pause-game? :player-item-on-cursor [_] true)
(defmethod pause-game? :player-idle           [_] true)
(defmethod pause-game? :player-dead           [_] true)

(defn- update-paused-state [{:keys [cdq.context/player-eid error] :as c} pausing?]
  (assoc c :cdq.context/paused? (or error
                                    (and pausing?
                                         (pause-game? (entity/state-obj @player-eid))
                                         (not (or (key-just-pressed? c :p)
                                                  (key-pressed? c :space)))))))

(defn- calculate-mouseover-eid [{:keys [cdq.context/player-eid] :as c}]
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities c (c/world-mouse-position c)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? c player @%))
         first)))

(defn- update-mouseover-entity [{:keys [cdq.context/mouseover-eid] :as c}]
  (let [new-eid (if (c/mouse-on-actor? c)
                  nil
                  (calculate-mouseover-eid c))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c ::mouseover-eid new-eid)))

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

(defn- check-player-input [{:keys [cdq.context/player-eid] :as c}]
  (component/manual-tick (entity/state-obj @player-eid)
                         c))

(def close-windows-key :escape)

(def window-hotkeys
  {:inventory-window   :i
   :entity-info-window :e})

(defn- game-loop [{:keys [gdl.context/world-viewport
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
                            {:controls/close-windows-key close-windows-key
                             :controls/window-hotkeys    window-hotkeys}
                            (c/stage c))
    c))

(defn -main []
  (let [config (read-edn-resource "app.edn")]
    (app/start (:app     config)
               (:context config)
               game-loop)))
