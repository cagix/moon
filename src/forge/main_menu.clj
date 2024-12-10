(ns forge.main-menu
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

; Remove main-menu:
; * how to exit the application -> inside the game itself (inventory, ability)
; * what happens if you die ?
; * which world to start at first ?
; * map-editor(mapgen-test) & property-editor have to go back to world screen
; => search :screens/main-menu occurences.

; How about the game itself is making the game?
; e.g. you can have an UI (restart/edit property)
; and can change the tiles
; place entities and edit their props, etc. ?
