(ns anvil.screens.main-menu
  (:require [anvil.db :as db]
            [anvil.graphics :as g]
            [anvil.input :refer [key-just-pressed?]]
            [anvil.screen :as screen]
            [anvil.stage :as stage]
            [anvil.ui :as ui :refer [ui-actor]]
            [anvil.utils :refer [dev-mode?]]
            [forge.world.create :refer [create-world]])
  (:import (com.badlogic.gdx Gdx)))

(deftype MainMenuScreen []
  screen/Screen
  (enter [_]
    (g/set-cursor :cursors/default))
  (exit [_])
  (dispose [_])
  (render [_]))

(defn create [background-image]
  (stage/screen :sub-screen (->MainMenuScreen)
                :actors [(background-image)
                         (ui/table
                          {:rows
                           (remove nil?
                                   (concat
                                    (for [world (db/build-all :properties/worlds)]
                                      [(ui/text-button (str "Start " (:property/id world))
                                                       #(do
                                                         (screen/change :screens/world)
                                                         (create-world world)))])
                                    [(when dev-mode?
                                       [(ui/text-button "Map editor"
                                                        #(screen/change :screens/map-editor))])
                                     (when dev-mode?
                                       [(ui/text-button "Property editor"
                                                        #(screen/change :screens/editor))])
                                     [(ui/text-button "Exit" #(.exit Gdx/app))]]))
                           :cell-defaults {:pad-bottom 25}
                           :fill-parent? true})
                         (ui-actor {:act (fn []
                                           (when (key-just-pressed? :keys/escape)
                                             (.exit Gdx/app)))})]))
