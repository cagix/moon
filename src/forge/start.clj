(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [forge.app :as app]
            [forge.db :as db]
            [forge.effects :as effects]
            [forge.entity :as entity]
            [forge.graphics :as graphics]
            [forge.screens.editor :as editor]
            [forge.screens.main :as main]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world]
            [forge.stage :as stage]
            [forge.ui :as ui]
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
                 (.setColor graphics/white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- make-cursors [cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                   cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
               (.dispose pixmap)
               cursor))
           cursors))

(defn -main []
  (let [{:keys [dock-icon
                lwjgl3
                db
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
         (db/init db)
         (bind-root #'asset-manager (load-assets assets))
         (bind-root #'graphics/batch (SpriteBatch.))
         (bind-root #'shape-drawer-texture (white-pixel-texture))
         (bind-root #'graphics/shape-drawer (ShapeDrawer. graphics/batch (TextureRegion. shape-drawer-texture 1 0 1 1)))
         (bind-root #'graphics/default-font (graphics/truetype-font
                                             {:file (.internal Gdx/files "fonts/exocet/films.EXL_____.ttf")
                                              :size 16
                                              :quality-scaling 2}))
         (bind-root #'graphics/world-unit-scale (float (/ tile-size)))
         (bind-root #'graphics/world-viewport (let [world-width  (* graphics/world-viewport-width  graphics/world-unit-scale)
                                                    world-height (* graphics/world-viewport-height graphics/world-unit-scale)
                                                    camera (OrthographicCamera.)
                                                    y-down? false]
                                                (.setToOrtho camera y-down? world-width world-height)
                                                (FitViewport. world-width world-height camera)))
         (bind-root #'graphics/cached-map-renderer (memoize
                                                    (fn [tiled-map]
                                                      (OrthogonalTiledMapRenderer. tiled-map
                                                                                   (float graphics/world-unit-scale)
                                                                                   graphics/batch))))
         (bind-root #'graphics/gui-viewport (FitViewport. graphics/gui-viewport-width
                                                          graphics/gui-viewport-height
                                                          (OrthographicCamera.)))
         (bind-root #'graphics/cursors (make-cursors cursors))
         (ui/init ui)
         (app/init-screens
          {:screens (mapvals stage/create
                             {:screens/main-menu  (main/create)
                              :screens/map-editor (map-editor/create)
                              :screens/editor     (editor/create)
                              :screens/minimap    (minimap/create)
                              :screens/world      (world/screen)})
           :first-screen-k :screens/main-menu}))

       (dispose []
         (.dispose asset-manager)
         (.dispose graphics/batch)
         (.dispose shape-drawer-texture)
         (.dispose graphics/default-font)
         (run! Disposable/.dispose (vals graphics/cursors))
         (ui/dispose)
         (app/dispose-screens))

       (render []
         (graphics/clear-screen)
         (app/render-current-screen))

       (resize [w h]
         (.update graphics/gui-viewport   w h true)
         (.update graphics/world-viewport w h)))
     (lwjgl3-config lwjgl3))))
