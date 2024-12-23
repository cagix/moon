(ns gdl.context.assets
  (:require [gdl.assets :as assets]
            [gdl.context :as ctx]))

(def assets-folder "resources/")

(defn setup []
  (bind-root ctx/assets (assets/manager assets-folder)))

(defn cleanup []
  (assets/cleanup ctx/assets))
