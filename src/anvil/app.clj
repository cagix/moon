(ns anvil.app
  (:require [anvil.controls :as controls]
            [anvil.db :as db]
            [anvil.lifecycle.create :refer [create-world dispose-world]]
            [anvil.lifecycle.render :refer [render-world]]
            [anvil.lifecycle.update :refer [update-world]]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.input :refer [key-just-pressed?]]
            [clojure.gdx.utils.screen-utils :as screen-utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.assets :as assets]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.ui.actor :refer [visible? set-visible] :as actor]
            [gdl.ui.group :refer [children]]))

(defn- windows []
  (:windows (stage/get)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? (get controls/window-hotkeys window-id))]
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
                    (screen-utils/clear color/black)
                    (stage/act)
                    (render-world)
                    (update-world)
                    (controls/adjust-zoom g/camera)
                    (check-window-hotkeys)
                    (when (controls/close-windows?)
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
