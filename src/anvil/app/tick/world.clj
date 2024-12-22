(ns anvil.app.tick.world
  (:require [anvil.app.tick :as tick]
            [anvil.component :as component]
            [anvil.controls :as controls]
            [anvil.entity :as entity]
            [anvil.lifecycle.potential-fields :refer [update-potential-fields!]]
            [anvil.world :as world :refer [mouseover-eid line-of-sight?]]
            [clojure.gdx.input :refer [key-just-pressed?]]
            [gdl.graphics :as g]
            [gdl.stage :as stage]
            [gdl.ui.actor :refer [visible? set-visible] :as actor]
            [gdl.ui.group :refer [children]]
            [gdl.utils :refer [bind-root sort-by-order defn-impl]]))

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
            (component/tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities [entities]
  (run! tick-entity entities))

(defn- calculate-eid []
  (let [player @world/player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (world/point->entities
                      (g/world-mouse-position)))]
    (->> world/render-z-order
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

(defmethod component/pause-game? :active-skill          [_] false)
(defmethod component/pause-game? :stunned               [_] false)
(defmethod component/pause-game? :player-moving         [_] false)
(defmethod component/pause-game? :player-item-on-cursor [_] true)
(defmethod component/pause-game? :player-idle           [_] true)
(defmethod component/pause-game? :player-dead           [_] true)

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? (get controls/window-hotkeys window-id))]
    (actor/toggle-visible! (get (:windows (stage/get)) window-id))))

(defn- close-all-windows []
  (let [windows (children (:windows (stage/get)))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(defn-impl tick/world []
  (component/manual-tick (entity/state-obj @world/player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root world/paused? (or world/error
                               (and pausing?
                                    (component/pause-game? (entity/state-obj @world/player-eid))
                                    (not (controls/unpaused?)))))
  (when-not world/paused?
    (let [delta-ms (min (g/delta-time) world/max-delta-time)]
      (alter-var-root #'world/elapsed-time + delta-ms)
      (bind-root world/delta-time delta-ms))
    (let [entities (world/active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (stage/error-window! t)
             (bind-root world/error t)))))
  (world/remove-destroyed-entities)  ; do not pause this as for example pickup item, should be destroyed.
  (controls/adjust-zoom g/camera)
  (check-window-hotkeys)
  (when (key-just-pressed? controls/close-windows-key)
    (close-all-windows)))
