(ns anvil.world.tick.window-hotkeys
  (:require [anvil.controls :as controls]
            [anvil.world.tick :as tick]
            [clojure.gdx.input :refer [key-just-pressed?]]
            [gdl.stage :as stage]
            [gdl.ui.actor :refer [visible? set-visible] :as actor]
            [gdl.ui.group :refer [children]]
            [gdl.utils :refer [defn-impl]]))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? (get controls/window-hotkeys window-id))]
    (actor/toggle-visible! (get (:windows (stage/get)) window-id))))

(defn- close-all-windows []
  (let [windows (children (:windows (stage/get)))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(defn-impl tick/window-hotkeys []
  (check-window-hotkeys)
  (when (key-just-pressed? controls/close-windows-key)
    (close-all-windows)))
