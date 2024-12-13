(ns anvil.app
  (:require anvil.effect.target.audiovisual
            anvil.effect.target.convert
            anvil.effect.target.damage
            anvil.effect.target.kill
            anvil.effect.target.melee-damage
            anvil.effect.target.spiderweb
            anvil.effect.target.stun

            anvil.effect.creature
            anvil.effect.projectile
            anvil.effect.target-all
            anvil.effect.target-entity

            anvil.entity.state.active-skill
            anvil.entity.state.npc-dead
            anvil.entity.state.npc-idle
            anvil.entity.state.npc-moving
            anvil.entity.state.npc-sleeping
            anvil.entity.state.player-dead
            anvil.entity.state.player-idle
            anvil.entity.state.player-item-on-cursor
            anvil.entity.state.player-moving
            anvil.entity.state.stunned

            anvil.entity.alert-friendlies-after-duration
            anvil.entity.animation
            anvil.entity.body
            anvil.entity.clickable
            anvil.entity.damage
            anvil.entity.delete-after-animation-stopped?
            anvil.entity.delete-after-duration
            anvil.entity.destroy-audiovisual
            anvil.entity.faction
            anvil.entity.fsm
            anvil.entity.hp
            anvil.entity.image
            anvil.entity.inventory
            anvil.entity.line-render
            anvil.entity.mana
            anvil.entity.modifiers
            anvil.entity.mouseover?
            anvil.entity.movement
            anvil.entity.projectile-collision
            anvil.entity.skills
            anvil.entity.species
            anvil.entity.stat
            anvil.entity.string-effect
            anvil.entity.temp-modifier

            ;; info stuff
            [anvil.component :as component]
            [anvil.entity.stat :as stat]
            [anvil.info :as info]
            [gdl.utils :refer [readable-number]]
            ;; info stuff

            [anvil.controls :as controls]
            [anvil.lifecycle.create :refer [create-world dispose-world]]
            [anvil.lifecycle.render :refer [render-world]]
            [anvil.lifecycle.update :refer [update-world]]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.assets :as assets]
            [gdl.db :as db]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [gdl.screen :as screen]
            [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.ui.actor :refer [visible? set-visible] :as actor]
            [gdl.ui.group :refer [children]]))

(defmethod component/info :property/pretty-name [[_ v]] v)
(defmethod component/info :maxrange             [[_ v]] v)

(defmethod component/info :creature/level [[_ v]]
  (str "Level: " v))

(defmethod component/info :projectile/piercing? [_] ; TODO also when false ?!
  "Piercing")

(defmethod component/info :skill/action-time-modifier-key [[_ v]]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod component/info :skill/action-time [[_ v]]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod component/info :skill/cooldown [[_ v]]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod component/info :skill/cost [[_ v]]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod component/info ::stat [[k _]]
  (str (info/k->pretty-name k) ": " (stat/->value info/*info-text-entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

; * Minimal dependencies editor (no world-viewport?, default-font,cursors?)

; * Mapgen Test (or test itself) a separate app make working - is the 'tests' app

; * Remove screens themself, tag it

; * Remove non-essential stuff (windows, widgets?, text?!)

; * When you die, restart world - needs to be an abstraction - so can be called
; from implementation component - ...

; * Move components out of defmulti namespaces

; * Info only for ingame is super idea

; * Stage knows about inventory/action-bar/modal/player-message!!

; * an extra layer _behind_ gdl can be a clojure.gdx or clojure.vis-ui
; but this comes later

; * it _already_ is an amazing library/framework because it works

; * next check our layers in anvil/forge - maybe effect will get
; a common API for world & gdl together?! -> doesn't need to know about gdl anymore?!

(defn- windows []
  (:windows (stage/get)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (children (windows))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(deftype WorldScreen []
  screen/Screen
  (enter [_]
    (cam/set-zoom! g/camera 0.8)) ; TODO no enter -> pass as arg to camera

  (exit [_]
    (g/set-cursor :cursors/default)) ; TODO no exit

  (render [_]
    (render-world)
    (update-world)
    (controls/adjust-zoom g/camera) ; TODO do I need adjust-zoom? no !
    (check-window-hotkeys)          ; do I need windows? no !
    (when (controls/close-windows?) ; no windows ! complicated! vampire survivors has no windows! although I like items -> open inventory there?
      (close-all-windows)))

  (dispose [_]
    (dispose-world)))

(defn world-screen []
  (stage/screen :sub-screen (->WorldScreen)))

(defn- start [{:keys [db app-config graphics ui world-id]}]
  (db/setup db)
  (lwjgl3/start app-config
                (reify lwjgl3/Application
                  (create [_]
                    (assets/setup)
                    (g/setup graphics)
                    (ui/setup ui)
                    (screen/setup {:screens/world (world-screen)}
                                  :screens/world)
                    (create-world (db/build world-id)))

                  (dispose [_]
                    (assets/cleanup)
                    (g/cleanup)
                    (ui/cleanup)
                    (screen/cleanup))

                  (render [_]
                    (g/clear)
                    (screen/render-current))

                  (resize [_ w h]
                    (g/resize w h)))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
