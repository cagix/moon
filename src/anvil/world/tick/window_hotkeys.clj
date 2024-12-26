(ns anvil.world.tick.window-hotkeys
  (:require [clojure.gdx :refer [key-just-pressed?]]
            [anvil.world.tick :as tick]))

(defn- check-window-hotkeys [c {:keys [controls/window-hotkeys]} stage]
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? c (get window-hotkeys window-id))]
    (toggle-visible! (get (:windows stage) window-id))))

(defn- close-all-windows [stage]
  (let [windows (children (:windows stage))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(defn-impl tick/window-hotkeys [c {:keys [controls/close-windows-key] :as controls} stage]
  (check-window-hotkeys c controls stage)
  (when (key-just-pressed? c close-windows-key)
    (close-all-windows stage)))
