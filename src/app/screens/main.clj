(ns ^:no-doc app.screens.main
  (:require [forge.db :as db]
            [forge.screen :as screen]
            [gdl.input :refer [key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.utils :refer [dev-mode?]]
            [forge.app :refer [change-screen set-cursor]]
            [app.screens.world :as world]
            [moon.widgets.background-image :as background-image])
  (:import (com.badlogic.gdx Gdx)))

(defn- exit []
  (.exit Gdx/app))

(defn- buttons []
  (ui/table
   {:rows
    (remove nil?
            (concat
             (for [world (db/all :properties/worlds)]
               [(ui/text-button (str "Start " (:property/id world))
                                #(world/start world))])
             [(when dev-mode?
                [(ui/text-button "Map editor" #(change-screen :screens/map-editor))])
              (when dev-mode?
                [(ui/text-button "Property editor" #(change-screen :screens/editor))])
              [(ui/text-button "Exit" exit)]]))
    :cell-defaults {:pad-bottom 25}
    :fill-parent? true}))

(deftype MainMenuScreen []
  screen/Screen
  (screen/enter [_]
    (set-cursor :cursors/default))
  (screen/exit [_])
  (screen/render [_])
  (screen/dispose [_]))

(defn create []
  {:actors [(background-image/create)
            (buttons)
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (exit)))})]
   :screen (->MainMenuScreen)})
