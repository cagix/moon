(ns ^:no-doc app.start
  (:require [app.editor :as editor]
            [app.screens.map-editor :as map-editor]
            [app.screens.minimap :as minimap]
            [app.world :as world]

            app.info

            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils :refer [mac? clear-screen]]
            [clojure.java.io :as io]
            [clojure.string :as str]

            [forge.app :as app]
            [forge.assets :as assets]
            [forge.graphics :as graphics]
            [forge.db :as db]
            [forge.graphics.cursors :as cursors]
            [forge.ui :as ui]
            [forge.utils :refer [dev-mode? mapvals]]

            (mapgen generate uf-caves tiled-map)

            [forge.effects :as effects]
            [forge.entity :as entity]
            [moon.systems.entity-state :as state]

            forge.entity.animation)
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
  {:optional [#'entity/->v
              #'entity/create
              #'entity/destroy
              #'entity/tick
              #'entity/render-below
              #'entity/render
              #'entity/render-above
              #'entity/render-info]})

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

(defn- background-image []
  (ui/image->widget (image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defn- main-menu []
  {:actors [(background-image)
            (ui/table
             {:rows
              (remove nil?
                      (concat
                       (for [world (db/all :properties/worlds)]
                         [(ui/text-button (str "Start " (:property/id world))
                                          #(world/start world))])
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
                              (when (gdx/key-just-pressed? :keys/escape)
                                (gdx/exit-app)))})]
   :screen (reify app/Screen
             (enter [_]
               (cursors/set :cursors/default))
             (exit [_])
             (render [_])
             (dispose [_]))})

(defn- editor-screen []
  {:actors [(background-image)
            (editor/tabs-table "[LIGHT_GRAY]Left-Shift: Back to Main Menu[]")
            (ui/actor {:act (fn []
                              (when (gdx/key-just-pressed? :shift-left)
                                (app/change-screen :screens/main-menu)))})]
   :screen (reify app/Screen
             (enter [_])
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
    (gdx/dispose stage)
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
                                               :screens/editor     (editor-screen)
                                               :screens/minimap    (minimap/create)
                                               :screens/world      (world/screen)}))
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
