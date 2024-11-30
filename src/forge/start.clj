(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [forge.app :as app]
            [forge.db :as db]
            [forge.editor :as editor]
            [forge.effects :as effects]
            [forge.entity :as entity]
            [forge.graphics :as g]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world]
            [forge.stage :as stage]
            [forge.ui :as ui]
            [forge.utils :refer [dev-mode?]]
            [moon.systems.entity-state :as state]
            [forge.entity.animation]
            [forge.info.impl]
            (mapgen generate uf-caves))
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture OrthographicCamera Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)
           (com.badlogic.gdx.utils Disposable SharedLibraryLoader)
           (com.badlogic.gdx.utils.viewport FitViewport)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)
           (space.earlygrey.shapedrawer ShapeDrawer)
           (forge OrthogonalTiledMapRenderer)))

(defn- background-image []
  (ui/image->widget (g/image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defn- main-screen []
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
                        [(ui/text-button "Exit" exit-app)]]))
              :cell-defaults {:pad-bottom 25}
              :fill-parent? true})
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (exit-app)))})]
   :screen (reify app/Screen
             (enter [_]
               (g/set-cursor :cursors/default))
             (exit [_])
             (render [_])
             (destroy [_]))})

(def tile-size 48)

(def ^:private config "app.edn")

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
   (install-component component-systems
                      ns-sym
                      (namespace->component-key #"^moon." (str ns-sym))))
  ([component-systems ns-sym k]
   (install-component component-systems ns-sym k)))

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

(defn- set-dock-icon [image-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource image-resource))))

(defn- lwjgl3-config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (FileHandle/.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- load-assets [folder]
  (let [manager (proxy [AssetManager clojure.lang.ILookup] []
                  (valAt [^String path]
                    (if (AssetManager/.contains this path)
                      (AssetManager/.get this path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[class exts] [[Sound   #{"wav"}]
                          [Texture #{"png" "bmp"}]]
            file (map #(str/replace-first % folder "")
                      (recursively-search (.internal Gdx/files folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(declare ^:private ^Texture shape-drawer-texture)

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (dispose pixmap)
    texture))

(defn- make-cursor [[file [hotspot-x hotspot-y]]]
  (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (dispose pixmap)
    cursor))

(defn -main []
  (let [{:keys [dock-icon
                lwjgl3
                db/schema
                db/properties
                assets
                cursors
                ui]} (-> config io/resource slurp edn/read-string)]
    (set-dock-icon dock-icon)
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
      (.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application.
     (proxy [ApplicationAdapter] []
       (create []
         (bind-root #'db/schemas (-> schema io/resource slurp edn/read-string))
         (bind-root #'db/properties-file (io/resource properties))
         (let [properties (-> db/properties-file slurp edn/read-string)]
           (assert (or (empty? properties)
                       (apply distinct? (map :property/id properties))))
           (run! db/validate! properties)
           (bind-root #'db/db (zipmap (map :property/id properties) properties)))
         (bind-root #'asset-manager (load-assets assets))
         (bind-root #'g/batch (SpriteBatch.))
         (bind-root #'shape-drawer-texture (white-pixel-texture))
         (bind-root #'g/shape-drawer (ShapeDrawer. g/batch (TextureRegion. shape-drawer-texture 1 0 1 1)))
         (bind-root #'g/default-font (g/truetype-font
                                      {:file (.internal Gdx/files "fonts/exocet/films.EXL_____.ttf")
                                       :size 16
                                       :quality-scaling 2}))
         (bind-root #'g/world-unit-scale (float (/ tile-size)))
         (bind-root #'g/world-viewport (let [world-width  (* g/world-viewport-width  g/world-unit-scale)
                                             world-height (* g/world-viewport-height g/world-unit-scale)
                                             camera (OrthographicCamera.)
                                             y-down? false]
                                         (.setToOrtho camera y-down? world-width world-height)
                                         (FitViewport. world-width world-height camera)))
         (bind-root #'g/cached-map-renderer (memoize
                                             (fn [tiled-map]
                                               (OrthogonalTiledMapRenderer. tiled-map
                                                                            (float g/world-unit-scale)
                                                                            g/batch))))
         (bind-root #'g/gui-viewport (FitViewport. g/gui-viewport-width
                                                   g/gui-viewport-height
                                                   (OrthographicCamera.)))
         (bind-root #'g/cursors (mapvals make-cursor cursors))
         (ui/init ui)
         (bind-root #'app/screens (mapvals stage/create
                                           {:screens/main-menu  (main-screen)
                                            :screens/map-editor (map-editor/create)
                                            :screens/editor     (editor/screen (background-image))
                                            :screens/minimap    (minimap/create)
                                            :screens/world      (world/screen)}))
         (app/change-screen :screens/main-menu))

       (dispose []
         (dispose asset-manager)
         (dispose g/batch)
         (dispose shape-drawer-texture)
         (dispose g/default-font)
         (run! dispose (vals g/cursors))
         (ui/destroy)
         (run! app/destroy (vals app/screens)))

       (render []
         (clear-screen black)
         (app/render (app/current-screen)))

       (resize [w h]
         (.update g/gui-viewport   w h true)
         (.update g/world-viewport w h)))
     (lwjgl3-config lwjgl3))))
