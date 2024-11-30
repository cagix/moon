(ns forge.start
  (:require [forge.entity :as entity]
            [forge.graphics :as g]
            [forge.info :as info :refer [info]]
            [forge.screens.editor :as editor]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world]
            [forge.screen :as screen]
            [forge.stage :as stage]
            [forge.ui :as ui]
            [forge.entity.components :refer [damage-mods hitpoints mana stat]]
            [forge.entity.state :as state]
            [forge.entity.animation]
            [forge.entity.render]
            [forge.operation :as op]
            (forge.mapgen generate
                          uf-caves)
            [forge.world :refer [finished-ratio]])
  (:import (com.badlogic.gdx ApplicationAdapter)
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

(g/add-color "PRETTY_NAME" [0.84 0.8 0.52])

(bind-root #'forge.info/info-color
           {:property/pretty-name "PRETTY_NAME"
            :entity/modifiers "CYAN"
            :maxrange "LIGHT_GRAY"
            :creature/level "GRAY"
            :projectile/piercing? "LIME"
            :skill/action-time-modifier-key "VIOLET"
            :skill/action-time "GOLD"
            :skill/cooldown "SKY"
            :skill/cost "CYAN"
            :entity/delete-after-duration "LIGHT_GRAY"
            :entity/faction "SLATE"
            :entity/fsm "YELLOW"
            :entity/species "LIGHT_GRAY"
            :entity/temp-modifier "LIGHT_GRAY"})

(bind-root #'forge.info/info-text-k-order
           [:property/pretty-name
            :skill/action-time-modifier-key
            :skill/action-time
            :skill/cooldown
            :skill/cost
            :skill/effects
            :entity/species
            :creature/level
            :entity/hp
            :entity/mana
            :entity/strength
            :entity/cast-speed
            :entity/attack-speed
            :entity/armor-save
            :entity/delete-after-duration
            :projectile/piercing?
            :entity/projectile-collision
            :maxrange
            :entity-effects])

(defmethod info :effects.target/convert [_]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info :effects.target/damage [[_ damage]]
  (damage-info damage)
  #_(if source
      (let [modified (damage-mods @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info :effects.target/kill [_]
  "Kills target")

; FIXME no source
(defmethod info :effects.target/melee-damage [_]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))
; => to entity move

(defmethod info :effects.target/spiderweb [_]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info :effects.target/stun [duration]
  (str "Stuns for " (readable-number duration) " seconds"))

(defmethod info :effects/target-all [_]
  "All visible targets")

(defmethod info :entity/delete-after-duration [counter]
  (str "Remaining: " (readable-number (finished-ratio counter)) "/1"))

(defmethod info :entity/faction [faction]
  (str "Faction: " (name faction)))

(defmethod info :entity/fsm [[_ fsm]]
  (str "State: " (name (:state fsm))))

(defmethod info :entity/hp [_]
  (str "Hitpoints: " (hitpoints info/*entity*)))

(defmethod info :entity/mana [_]
  (str "Mana: " (mana info/*entity*)))

(defn- +? [n]
  (case (signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defsystem op-value-text)

(defmethod op-value-text :op/inc [[_ value]]
  (str value))

(defmethod op-value-text :op/mult [[_ value]]
  (str value "%"))

(defn- ops-info [ops k]
  (str-join "\n"
            (keep
             (fn [{v 1 :as op}]
               (when-not (zero? v)
                 (str (+? v) (op-value-text op) " " (k->pretty-name k))))
             (sort-by op/order ops))))

(defmethod info :entity/modifiers [[_ mods]]
  (when (seq mods)
    (str-join "\n" (keep (fn [[k ops]]
                           (ops-info ops k)) mods))))

#_(defmethod info [skills]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str-join "," (map name (keys skills))))))

(defmethod info :entity/species [[_ species]]
  (str "Creature - " (capitalize (name species))))

(defmethod info :entity/temp-modifier [[_ {:keys [counter]}]]
  (str "Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1"))

(defmethod info :property/pretty-name [[_ v]] v)
(defmethod info :maxrange             [[_ v]] v)

(defmethod info :creature/level [[_ v]]
  (str "Level: " v))

(defmethod info :projectile/piercing? [_] ; TODO also when false ?!
  "Piercing")

(defmethod info :skill/action-time-modifier-key [[_ v]]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info :skill/action-time [[_ v]]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info :skill/cooldown [[_ v]]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info :skill/cost [[_ v]]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info ::stat [[k _]]
  (str (k->pretty-name k) ": " (stat info/*entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

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
                       (for [world (build-all :properties/worlds)]
                         [(ui/text-button (str "Start " (:property/id world))
                                          #(world/start world))])
                       [(when dev-mode?
                          [(ui/text-button "Map editor"
                                           #(change-screen :screens/map-editor))])
                        (when dev-mode?
                          [(ui/text-button "Property editor"
                                           #(change-screen :screens/editor))])
                        [(ui/text-button "Exit" exit-app)]]))
              :cell-defaults {:pad-bottom 25}
              :fill-parent? true})
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (exit-app)))})]
   :screen (reify screen/Screen
             (enter [_]
               (g/set-cursor :cursors/default))
             (exit [_])
             (render [_])
             (destroy [_]))})

(def tile-size 48)

(def ^:private config "app.edn")

(defn- namespace->component-key [prefix ns-str]
   (let [ns-parts (-> ns-str
                      (str-replace prefix "")
                      (str-split #"\."))]
     (keyword (str-join "." (drop-last ns-parts))
              (last ns-parts))))

(comment
 (and (= (namespace->component-key #"^forge." "forge.effects.projectile")
         :effects/projectile)
      (= (namespace->component-key #"^forge." "forge.effects.target.convert")
         :effects.target/convert)))

(defn- install
  ([component-systems ns-sym]
   (install-component component-systems
                      ns-sym
                      (namespace->component-key #"^forge." (str ns-sym))))
  ([component-systems ns-sym k]
   (install-component component-systems ns-sym k)))

(defn- install-all [component-systems ns-syms]
  (doseq [ns-sym ns-syms]
    (install component-systems ns-sym)))

(def ^:private effect
  {:required [#'applicable?
              #'handle]
   :optional [#'useful?
              #'render-effect]})

(install-all effect '[forge.effects.projectile
                      forge.effects.spawn
                      forge.effects.target-all
                      forge.effects.target-entity
                      forge.effects.target.audiovisual
                      forge.effects.target.convert
                      forge.effects.target.damage
                      forge.effects.target.kill
                      forge.effects.target.melee-damage
                      forge.effects.target.spiderweb
                      forge.effects.target.stun])

(def ^:private entity
  {:optional [#'entity/->v
              #'entity/create
              #'entity/destroy
              #'entity/tick
              #'entity/render-below
              #'entity/render
              #'entity/render-above
              #'entity/render-info]})

(install-all entity '[forge.entity.alert-friendlies-after-duration
                      forge.entity.delete-after-duration
                      forge.entity.destroy-audiovisual
                      forge.entity.fsm
                      forge.entity.inventory
                      forge.entity.projectile-collision
                      forge.entity.skills
                      forge.entity.string-effect
                      forge.entity.movement
                      forge.entity.temp-modifier
                      forge.entity.hp
                      forge.entity.mana])

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

(install entity-state 'forge.entity.npc.dead              :npc-dead)
(install entity-state 'forge.entity.npc.idle              :npc-idle)
(install entity-state 'forge.entity.npc.moving            :npc-moving)
(install entity-state 'forge.entity.npc.sleeping          :npc-sleeping)
(install entity-state 'forge.entity.player.dead           :player-dead)
(install entity-state 'forge.entity.player.idle           :player-idle)
(install entity-state 'forge.entity.player.item-on-cursor :player-item-on-cursor)
(install entity-state 'forge.entity.player.moving         :player-moving)
(install entity-state 'forge.entity.active                :active-skill)
(install entity-state 'forge.entity.stunned               :stunned)

(defn- set-dock-icon [image-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io-resource image-resource))))

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
            file (map #(str-replace-first % folder "")
                      (recursively-search (internal-file folder) exts))]
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

(defn -main []
  (let [{:keys [dock-icon
                lwjgl3
                db/schema
                db/properties
                assets
                cursors
                ui]} (-> config io-resource slurp edn-read-string)]
    (bind-root #'db-schemas (-> schema io-resource slurp edn-read-string))
    (bind-root #'db-properties-file (io-resource properties))
    (let [properties (-> db-properties-file slurp edn-read-string)]
      (assert (or (empty? properties)
                  (apply distinct? (map :property/id properties))))
      (run! validate! properties)
      (bind-root #'db-properties (zipmap (map :property/id properties) properties)))
    (set-dock-icon dock-icon)
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
      (.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application.
     (proxy [ApplicationAdapter] []
       (create []
         (bind-root #'asset-manager (load-assets assets))
         (bind-root #'g/batch (SpriteBatch.))
         (bind-root #'shape-drawer-texture (white-pixel-texture))
         (bind-root #'g/shape-drawer (ShapeDrawer. g/batch (TextureRegion. shape-drawer-texture 1 0 1 1)))
         (bind-root #'g/default-font (g/truetype-font
                                      {:file (internal-file "fonts/exocet/films.EXL_____.ttf")
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
         (bind-root #'g/cursors (mapvals gdx-cursor cursors))
         (ui/init ui)
         (bind-root #'screens (mapvals stage/create
                                       {:screens/main-menu  (main-screen)
                                        :screens/map-editor (map-editor/create)
                                        :screens/editor     (editor/screen (background-image))
                                        :screens/minimap    (minimap/create)
                                        :screens/world      (world/screen)}))
         (change-screen :screens/main-menu))

       (dispose []
         (dispose asset-manager)
         (dispose g/batch)
         (dispose shape-drawer-texture)
         (dispose g/default-font)
         (run! dispose (vals g/cursors))
         (ui/destroy)
         (run! screen/destroy (vals screens)))

       (render []
         (clear-screen black)
         (screen/render (current-screen)))

       (resize [w h]
         (.update g/gui-viewport   w h true)
         (.update g/world-viewport w h)))
     (lwjgl3-config lwjgl3))))
