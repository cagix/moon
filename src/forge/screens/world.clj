(ns forge.screens.world
  (:require [anvil.app :refer [change-screen]]
            [anvil.controls :as controls]
            [anvil.fsm :as fsm]
            [anvil.graphics :as g :refer [set-cursor world-mouse-position world-camera]]
            [anvil.screen :refer [Screen]]
            [anvil.stage :as stage]
            [anvil.system :as system]
            [anvil.world :as world :refer [player-eid explored-tile-corners mouseover-entity mouseover-eid active-entities circle->cells point->entities line-of-sight? render-z-order]]
            [clojure.gdx.graphics :refer [clear-screen delta-time]]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.scene2d.actor :refer [visible? set-visible] :as actor]
            [clojure.gdx.scene2d.group :refer [children]]
            [clojure.utils :refer [bind-root ->tile sort-by-order]]
            [forge.world.create :refer [start-world dispose-world]]
            [forge.world.render :refer [render-world]]
            [forge.world.potential-fields :refer [update-potential-fields!]]))

(defn- windows []
  (:windows (stage/get)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (children (windows))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (system/tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities [entities]
  (run! tick-entity entities))

(defn- time-update []
  (let [delta-ms (min (delta-time) world/max-delta-time)]
    (alter-var-root #'world/elapsed-time + delta-ms)
    (bind-root world/world-delta delta-ms)))

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities
                      (world-mouse-position)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root mouseover-eid new-eid)))

(defn- remove-destroyed-entities []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (world/all-entities))]
    (world/remove-entity eid)
    (doseq [component @eid]
      (system/destroy component eid))))

(defn- update-world []
  (system/manual-tick (fsm/state-obj @player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root world/paused? (or world/tick-error
                               (and pausing?
                                    (system/pause-game? (fsm/state-obj @player-eid))
                                    (not (controls/unpaused?)))))
  (when-not world/paused?
    (time-update)
    (let [entities (active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (stage/error-window! t)
             (bind-root world/tick-error t)))))
  (remove-destroyed-entities)) ; do not pause this as for example pickup item, should be destroyed.


(deftype WorldScreen []
  Screen
  (enter [_]
    (cam/set-zoom! (world-camera) 0.8))

  (exit [_]
    (set-cursor :cursors/default))

  (render [_]
    (clear-screen color/black)
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (change-screen :screens/minimap)))

  (dispose [_]
    (dispose-world)))

(defn create []
  (stage/create
   {:screen (->WorldScreen)}))
