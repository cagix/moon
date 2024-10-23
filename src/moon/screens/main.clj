(ns moon.screens.main
  (:require [clojure.gdx :refer [exit-app key-just-pressed?]]
            [moon.screens.world :as world]
            [moon.db :as db]
            [moon.graphics :as g]
            [moon.screen :as screen]
            [moon.ui :as ui]
            [moon.ui.stage-screen :as stage-screen]
            [moon.utils :refer [dev-mode?]]))

(defn- ->buttons []
  (ui/table {:rows (remove nil? (concat
                                 (for [{:keys [property/id]} (db/all :properties/worlds)]
                                   [(ui/text-button (str "Start " id) (world/start-game-fn id))])
                                 [(when dev-mode?
                                    [(ui/text-button "Map editor" #(screen/change! :screens/map-editor))])
                                  (when dev-mode?
                                    [(ui/text-button "Property editor" #(screen/change! :screens/property-editor))])
                                  [(ui/text-button "Exit" exit-app)]]))
             :cell-defaults {:pad-bottom 25}
             :fill-parent? true}))

(deftype MainMenuScreen []
  screen/Screen
  (screen/enter! [_]
    (g/set-cursor! :cursors/default))
  (screen/exit! [_])
  (screen/render! [_])
  (screen/dispose! [_]))

(defn create [background-image]
  [:screens/main-menu
   (stage-screen/create :actors
                        [(background-image)
                         (->buttons)
                         (ui/actor {:act (fn []
                                           (when (key-just-pressed? :keys/escape)
                                             (exit-app)))})]
                        :screen (->MainMenuScreen))])
