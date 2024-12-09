(ns forge.app.asset-manager
  (:require [anvil.app :as app]
            [anvil.assets :as assets]
            [anvil.disposable :as disposable]
            [anvil.files :as files]
            [clojure.string :as str]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ folder]]
  (bind-root app/assets (assets/load-all
                         (for [[asset-type exts] [[:sound   #{"wav"}]
                                                  [:texture #{"png" "bmp"}]]
                               file (map #(str/replace-first % folder "")
                                         (files/recursively-search folder exts))]
                           [file asset-type]))))

(defn dispose [_]
  (disposable/dispose app/assets))
