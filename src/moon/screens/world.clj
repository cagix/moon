(ns ^:no-doc moon.screens.world
  (:require [clj-commons.pretty.repl :refer [pretty-pst]]
            [gdl.graphics :refer [clear-screen frames-per-second delta-time]]
            [gdl.graphics.camera :as cam]
            [gdl.input :refer [key-pressed? key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [gdl.ui.stage]
            [gdl.utils :refer [dev-mode? sort-by-order]]
            [moon.component :refer [defc] :as component]
            [moon.db :as db]
            [moon.entity :as entity]
            [moon.graphics :as g]
            [moon.level :as level]
            [moon.screen :as screen]
            [moon.stage :as stage]
            [moon.widgets.error-window :refer [error-window!]]
            [moon.world :as world]
            [moon.world.debug-render :as debug-render]
            [moon.world.potential-fields :refer [update-potential-fields!]]))

(defn- calculate-mouseover-eid []
  (let [player @world/player
        hits (remove #(= (:z-order @%) :z-order/effect) ; or: only items/creatures/projectiles.
                     (world/point->entities
                      (g/world-mouse-position)))]
    (->> entity/render-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(world/line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity! []
  (let [eid (if (stage/mouse-on-actor?)
              nil
              (calculate-mouseover-eid))]
    [(when world/mouseover-eid
       [:e/dissoc world/mouseover-eid :entity/mouseover?])
     (when eid
       [:e/assoc eid :entity/mouseover? true])
     (fn []
       (.bindRoot #'world/mouseover-eid eid)
       nil)]))

(defn- check-window-hotkeys []
  (doseq [[hotkey window-id] {:keys/i :inventory-window
                              :keys/e :entity-info-window}
          :when (key-just-pressed? hotkey)]
    (a/toggle-visible! (get (:windows (stage/get)) window-id))))

(defn- close-windows?! []
  (let [windows (ui/children (:windows (stage/get)))]
    (if (some a/visible? windows)
      (do
       (run! #(a/set-visible! % false) windows)
       true))))

(defn- adjust-zoom [camera by] ; DRY map editor
  (cam/set-zoom! camera (max 0.1 (+ (cam/zoom camera) by))))

(def ^:private zoom-speed 0.05)

(defn- check-zoom-keys []
  (let [camera (g/world-camera)]
    (when (key-pressed? :keys/minus)  (adjust-zoom camera    zoom-speed))
    (when (key-pressed? :keys/equals) (adjust-zoom camera (- zoom-speed)))))

; TODO move to actor/stage listeners ? then input processor used ....
(defn- check-key-input []
  (check-zoom-keys)
  (check-window-hotkeys)
  (cond (key-just-pressed? :keys/escape)
        (close-windows?!)

        ; TODO not implementing StageSubScreen so NPE no screen-render!
        #_(key-just-pressed? :keys/tab)
        #_(screen/change! :screens/minimap)))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (g/draw-rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) :white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t 12))))

(defn- render-entities!
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @world/player]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              entity/render-order)
            system entity/render-systems
            entity entities
            :when (or (= z-order :z-order/effect)
                      (world/line-of-sight? player entity))]
      (render-entity! system entity))))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- player-state-pause-game? [] (entity/pause-game? (entity/state-obj @world/player)))
(defn- player-update-state      [] (entity/manual-tick (entity/state-obj @world/player)))

(defn- player-unpaused? []
  (or (key-just-pressed? :keys/p)
      (key-pressed? :keys/space))) ; FIXMe :keys? shouldnt it be just :space?

(defn- update-game-paused []
  (.bindRoot #'world/paused? (or world/entity-tick-error
                                 (and pausing?
                                      (player-state-pause-game?)
                                      (not (player-unpaused?)))))
  nil)

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (when-let [v (k @eid)]
       (component/->handle
        (try (entity/tick [k v] eid)
             (catch Throwable t
               (throw (ex-info "entity/tick" {:k k} t)))))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(deftype WorldScreen []
  screen/Screen
  (screen/enter! [_])

  (screen/exit! [_]
    (g/set-cursor! :cursors/default))

  (screen/render! [_]
    (clear-screen :black)
    ; FIXME position DRY
    (cam/set-position! (g/world-camera) (:position @world/player))
    ; FIXME position DRY
    (world/render-tiled-map! (cam/position (g/world-camera)))
    (g/render-world-view! (fn []
                            (debug-render/before-entities)
                            ; FIXME position DRY (from player)
                            (render-entities! (map deref (world/active-entities)))
                            (debug-render/after-entities)))
    (component/->handle
     [player-update-state
      ; this do always so can get debug info even when game not running
      update-mouseover-entity!
      update-game-paused
      #(when-not world/paused?
         (alter-var-root #'world/logic-frame inc)
         (let [delta (min (delta-time) entity/max-delta-time)]
           (.bindRoot      #'world/delta-time delta)
           (alter-var-root #'world/elapsed-time + delta))
         (let [entities (world/active-entities)]
           (update-potential-fields! entities)
           (try (run! tick-entity entities)
                (catch Throwable t
                  (error-window! t)
                  (.bindRoot #'world/entity-tick-error t))))
         nil)
      ; do not pause this as for example pickup item, should be destroyed.
      [:tx/remove-destroyed-entities]])
    (check-key-input))

  (screen/dispose! [_]
    (world/clear-tiled-map)))

(defc :screens/world
  (component/create [_]
    (stage/create :screen (->WorldScreen))))

(defn- world-actors []
  [(if dev-mode?
     (component/create [:widgets/dev-menu nil])
     (ui/actor {}))
   (ui/table {:rows [[{:actor (component/create [:widgets/action-bar nil])
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (component/create [:widgets/hp-mana nil])
   (ui/group {:id :windows
              :actors [(component/create [:widgets/entity-info-window nil])
                       (component/create [:widgets/inventory          nil])]})
   (component/create [:widgets/draw-item-on-cursor nil])
   (component/create [:widgets/player-message      nil])])

(defn- reset-stage! []
  (let [stage (stage/get)]
    (gdl.ui.stage/clear! stage)
    (run! #(gdl.ui.stage/add! stage %) (world-actors))))

(.bindRoot #'world/start
           (fn start-game-fn [world-id]
             (screen/change! :screens/world)
             (reset-stage!)
             (let [level (level/generate world-id)]
               (world/init! (:tiled-map level))
               (world/spawn-entities level))))
