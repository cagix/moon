(ns anvil.app
  (:require [anvil.controls :as controls]
            [anvil.db :as db]
            [anvil.lifecycle.create :refer [create-world dispose-world]]
            [anvil.lifecycle.render :refer [render-world]]
            [anvil.lifecycle.update :refer [update-world]]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.assets :as assets]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.ui.actor :refer [visible? set-visible] :as actor]
            [gdl.ui.group :refer [children]]))

; * Minimal dependencies editor (no world-viewport?, default-font,cursors?)

; * Mapgen Test (or test itself) a separate app make working - is the 'tests' app

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

(defn- start [{:keys [requires db app-config graphics ui world-id]}]
  (run! require requires)
  (db/setup db)
  (lwjgl3/start app-config
                (reify lwjgl3/Application
                  (create [_]
                    (assets/setup)
                    (g/setup graphics)
                    (cam/set-zoom! g/camera 0.8)
                    (ui/setup ui)
                    (stage/setup)
                    (create-world (db/build world-id)))

                  (dispose [_]
                    (assets/cleanup)
                    (g/cleanup)
                    (ui/cleanup)
                    (stage/cleanup)
                    (dispose-world))

                  (render [_]
                    (g/clear)
                    (stage/act)
                    (render-world)
                    (update-world)
                    (controls/adjust-zoom g/camera) ; TODO do I need adjust-zoom? no !
                    (check-window-hotkeys)          ; do I need windows? no !
                    (when (controls/close-windows?) ; no windows ! complicated! vampire survivors has no windows! although I like items -> open inventory there?
                      (close-all-windows))
                    (stage/render))

                  (resize [_ w h]
                    (g/resize w h)))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
