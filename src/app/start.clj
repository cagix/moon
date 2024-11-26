(ns ^:no-doc app.start
  (:require [forge.app :as app]
            [forge.assets :as assets]
            [app.screens.editor :as editor]
            [app.screens.map-editor :as map-editor]
            [app.screens.minimap :as minimap]
            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils :refer [dispose mac? clear-screen]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [forge.graphics :as graphics :refer [draw-tiled-map draw-on-world-view gui-mouse-position world-camera world-mouse-position]]
            [forge.db :as db]
            [forge.graphics.cursors :as cursors]
            [forge.level :as level]
            (forge.schema boolean enum image map number one-to-many one-to-one sound string)
            [forge.widgets.error-window :refer [error-window!]]
            [forge.graphics.camera :as cam]
            [forge.input :refer [key-just-pressed?]]
            [forge.ui :as ui]
            [forge.ui.actor :as actor]
            [forge.utils :refer [readable-number dev-mode? mapvals]]
            (mapgen generate uf-caves tiled-map)
            [moon.controls :as controls]
            [moon.entity :as entity]
            [forge.effects :as effects]
            [forge.entity :as entity-sys]
            [moon.systems.entity-state :as state]
            moon.methods.info

            forge.entity.animation

            [moon.widgets.background-image :as background-image]
            [moon.widgets.action-bar :as action-bar]
            [moon.widgets.dev-menu :as dev-menu]
            [moon.widgets.entity-info-window :as entity-info-window]
            [moon.widgets.hp-mana :as hp-mana]
            [moon.widgets.inventory :as inventory]
            [moon.widgets.player-message :as player-message]
            [moon.world :as world :refer [tick-error paused? player-eid]]
            [moon.world.debug-render :as debug-render]
            [moon.world.mouseover :as mouseover]
            [moon.world.potential-fields :refer [update-potential-fields!]]
            [moon.world.tile-color-setter :as tile-color-setter])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(def ^:private no-doc? true)

; check fn-params ... ? compare with sys-params ?
; #_(first (:arglists (meta #'render)))
(defn- add-method [system-var k avar]
  (assert (keyword? k))
  (assert (var? avar) (pr-str avar))
  (if no-doc?
    (alter-meta! avar assoc :no-doc true)
    (alter-meta! avar update :doc str "\n installed as defmethod for " system-var))
  (let [system @system-var]
    (when (k (methods system))
      (println "WARNING: Overwriting method" (:name (meta avar)) "on" k))
    (clojure.lang.MultiFn/.addMethod system k (fn call-method [[k & vs] & args]
                                                (binding [*k* k]
                                                  (apply avar (into (vec vs) args)))))))

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars
          :let [method-var (ns-resolve ns-sym (:name (meta system-var)))]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))
    (when method-var
      (add-method system-var k method-var))))

(defn- ns-publics-without-no-doc? [ns]
  (some #(not (:no-doc (meta %))) (vals (ns-publics ns))))

(defn- install* [component-systems ns-sym k]
  (require ns-sym)
  (add-methods (:required component-systems) ns-sym k)
  (add-methods (:optional component-systems) ns-sym k :optional? true)
  (let [ns (find-ns ns-sym)]
    (if (and no-doc? (not (ns-publics-without-no-doc? ns)))
      (alter-meta! ns assoc :no-doc true)
      (alter-meta! ns update :doc str "\n component: `" k "`"))))

(defn- namespace->component-key [prefix ns-str]
   (let [ns-parts (-> ns-str
                      (str/replace prefix "")
                      (str/split #"\."))]
     (keyword (str/join "." (drop-last ns-parts))
              (last ns-parts))))

(comment
 (and (= (namespace->component-key "moon.effects.projectile")
         :effects/projectile)
      (= (namespace->component-key "moon.effects.target.convert")
         :effects.target/convert)))

(defn- install
  ([component-systems ns-sym]
   (install* component-systems
             ns-sym
             (namespace->component-key #"^moon." (str ns-sym))))
  ([component-systems ns-sym k]
   (install* component-systems ns-sym k)))

(defn- install-all [component-systems ns-syms]
  (doseq [ns-sym ns-syms]
    (install component-systems ns-sym)))

(declare start-world)

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor?)]
    (str "TRUE - name:" (.getName actor)
         "id: " (actor/id actor)))

(defn- dev-menu-bar []
  (dev-menu/create
   {:menus [{:label "Screens"
             :items [{:label "Map-editor"
                      :on-click (partial app/change-screen :screens/map-editor)}
                     {:label "Editor"
                      :on-click (partial app/change-screen :screens/editor)}
                     {:label "Main-Menu"
                      :on-click (partial app/change-screen :screens/main-menu)}]}
            {:label "World"
             :items (for [world (db/all :properties/worlds)]
                      {:label (str "Start " (:property/id world))
                       :on-click #(start-world world)})}
            {:label "Help"
             :items [{:label controls/help-text}]}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn #(when-let [entity (mouseover/entity)] (:entity/id entity))
                     :icon "images/mouseover.png"}
                    {:label "elapsed-time"
                     :update-fn #(str (readable-number world/elapsed-time) " seconds")
                     :icon "images/clock.png"}
                    {:label "paused?"
                     :update-fn (fn [] paused?)}
                    {:label "GUI"
                     :update-fn gui-mouse-position}
                    {:label "World"
                     :update-fn #(mapv int (world-mouse-position))}
                    {:label "Zoom"
                     :update-fn #(cam/zoom (world-camera))
                     :icon "images/zoom.png"}
                    {:label "FPS"
                     :update-fn gdx/frames-per-second
                     :icon "images/fps.png"}]}))

(defn- dev-menu []
  (ui/table {:rows [[{:actor (.getTable (dev-menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (actor/set-touchable! :disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

; FIXME camera/viewport used @ line of sight & raycaster explored tiles
; fixed player viewing range use & for opponents too

(defn- widgets []
  [(if dev-mode?
     (dev-menu)
     (ui/actor {}))
   (ui/table {:rows [[{:actor (action-bar/create)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (hp-mana/actor)
   (ui/group {:id :windows
              :actors [(entity-info-window/create)
                       (inventory/create)]})
   (ui/actor {:draw #(state/draw-gui-view (entity/state-obj @player-eid))})
   (player-message/create)])

(defn- windows []
  (:windows (app/stage)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (ui/children (windows))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible! % false) windows))))

(defn- start-world [world-props]
  (app/change-screen :screens/world)
  (stage/clear (app/stage))
  (run! #(stage/add (app/stage) %) (widgets))
  (world/clear)
  (world/init (level/generate world-props)))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- update-world []
  (state/manual-tick (entity/state-obj @player-eid))
  (mouseover/update) ; this do always so can get debug info even when game not running
  (.bindRoot #'paused? (or tick-error
                           (and pausing?
                                (state/pause-game? (entity/state-obj @player-eid))
                                (not (controls/unpaused?)))))
  (when-not paused?
    (let [delta-ms (min (gdx/delta-time) world/max-delta-time)]
      (alter-var-root #'world/elapsed-time + delta-ms)
      (.bindRoot #'world/delta delta-ms) )
    (let [entities (world/active-entities)]
      (update-potential-fields! entities)
      (try (world/tick-entities entities)
           (catch Throwable t
             (error-window! t)
             (.bindRoot #'tick-error t)))))
  (world/remove-destroyed)) ; do not pause this as for example pickup item, should be destroyed.

(defn- render-world []
  ; FIXME position DRY
  (cam/set-position! (world-camera) (:position @player-eid))
  ; FIXME position DRY
  (draw-tiled-map world/tiled-map
                  (tile-color-setter/create
                   world/explored-tile-corners
                   (cam/position (world-camera))))
  (draw-on-world-view (fn []
                       (debug-render/before-entities)
                       ; FIXME position DRY (from player)
                       (world/render-entities (map deref (world/active-entities)))
                       (debug-render/after-entities))))

(deftype WorldScreen []
  app/Screen
  (enter [_]
    (cam/set-zoom! (world-camera) 0.8))

  (exit [_]
    (cursors/set :cursors/default))

  (render [_]
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (app/change-screen :screens/minimap)))

  (dispose [_]
    (world/clear)))

(defn- world-screen []
  {:screen (->WorldScreen)})

(def ^:private effect
  {:required [#'effects/applicable?
              #'effects/handle]
   :optional [#'effects/useful?
              #'effects/render]})

(install-all effect '[moon.effects.projectile
                      moon.effects.spawn
                      moon.effects.target-all
                      moon.effects.target-entity
                      moon.effects.target.audiovisual
                      moon.effects.target.convert
                      moon.effects.target.damage
                      moon.effects.target.kill
                      moon.effects.target.melee-damage
                      moon.effects.target.spiderweb
                      moon.effects.target.stun])

(def ^:private entity
  {:optional [#'entity-sys/->v
              #'entity-sys/create
              #'entity-sys/destroy
              #'entity-sys/tick
              #'entity-sys/render-below
              #'entity-sys/render
              #'entity-sys/render-above
              #'entity-sys/render-info]})

(install-all entity '[moon.entity.alert-friendlies-after-duration
                      moon.entity.clickable
                      moon.entity.delete-after-duration
                      moon.entity.destroy-audiovisual
                      moon.entity.fsm
                      moon.entity.image
                      moon.entity.inventory
                      moon.entity.line-render
                      moon.entity.mouseover?
                      moon.entity.projectile-collision
                      moon.entity.skills
                      moon.entity.string-effect
                      moon.entity.movement
                      moon.entity.temp-modifier
                      moon.entity.hp
                      moon.entity.mana])

(def ^:private entity-state
  (merge-with concat
              entity
              {:optional [#'state/enter
                          #'state/exit
                          #'state/cursor
                          #'state/pause-game?
                          #'state/manual-tick
                          #'state/clicked-inventory-cell
                          #'state/clicked-skillmenu-skill
                          #'state/draw-gui-view]}))

(install entity-state 'moon.entity.npc.dead              :npc-dead)
(install entity-state 'moon.entity.npc.idle              :npc-idle)
(install entity-state 'moon.entity.npc.moving            :npc-moving)
(install entity-state 'moon.entity.npc.sleeping          :npc-sleeping)
(install entity-state 'moon.entity.player.dead           :player-dead)
(install entity-state 'moon.entity.player.idle           :player-idle)
(install entity-state 'moon.entity.player.item-on-cursor :player-item-on-cursor)
(install entity-state 'moon.entity.player.moving         :player-moving)
(install entity-state 'moon.entity.active                :active-skill)
(install entity-state 'moon.entity.stunned               :stunned)

(defn- main-menu []
  {:actors [(background-image/create)
            (ui/table
             {:rows
              (remove nil?
                      (concat
                       (for [world (db/all :properties/worlds)]
                         [(ui/text-button (str "Start " (:property/id world))
                                          #(start-world world))])
                       [(when dev-mode?
                          [(ui/text-button "Map editor"
                                           #(app/change-screen :screens/map-editor))])
                        (when dev-mode?
                          [(ui/text-button "Property editor"
                                           #(app/change-screen :screens/editor))])
                        [(ui/text-button "Exit"
                                         gdx/exit-app)]]))
              :cell-defaults {:pad-bottom 25}
              :fill-parent? true})
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (gdx/exit-app)))})]
   :screen (reify app/Screen
             (enter [_]
               (cursors/set :cursors/default))
             (exit [_])
             (render [_])
             (dispose [_]))})

(defn- set-dock-icon [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))

(defrecord StageScreen [stage sub-screen]
  app/Screen
  (enter [_]
    (gdx/set-input-processor stage)
    (app/enter sub-screen))

  (exit [_]
    (gdx/set-input-processor nil)
    (app/exit sub-screen))

  (render [_]
    (stage/act stage)
    (app/render sub-screen)
    (stage/draw stage))

  (dispose [_]
    (dispose stage)
    (app/dispose sub-screen)))

(defn- stage-create [viewport batch]
  (proxy [com.badlogic.gdx.scenes.scene2d.Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (ui/find-actor-with-id (.getRoot this) id))
      ([id not-found]
       (or (ui/find-actor-with-id (.getRoot this) id)
           not-found)))))

(defn- stage-screen
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (stage-create graphics/gui-viewport graphics/batch)]
    (run! #(stage/add stage %) actors)
    (->StageScreen stage screen)))

(defn -main []
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (set-dock-icon "moon.png")
  (when mac?
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (lwjgl3/application (proxy [ApplicationAdapter] []
                        (create []
                          (assets/init)
                          (cursors/init)
                          (graphics/init)
                          (ui/load! :skin-scale/x1)
                          (.bindRoot #'app/screens
                                     (mapvals stage-screen
                                              {:screens/main-menu  (main-menu)
                                               :screens/map-editor (map-editor/create)
                                               :screens/editor     (editor/create)
                                               :screens/minimap    (minimap/create)
                                               :screens/world      (world-screen)}))
                          (app/change-screen :screens/main-menu))

                        (dispose []
                          (assets/dispose)
                          (cursors/dispose)
                          (graphics/dispose)
                          (run! app/dispose (vals app/screens))
                          (ui/dispose!))

                        (render []
                          (clear-screen color/black)
                          (app/render (app/current-screen)))

                        (resize [w h]
                          (.update graphics/gui-viewport   w h true)
                          (.update graphics/world-viewport w h)))
                      (lwjgl3/config {:title "Moon"
                                      :fps 60
                                      :width 1440
                                      :height 900})))
