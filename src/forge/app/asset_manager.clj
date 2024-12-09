(ns forge.app.asset-manager
  (:require [anvil.assets :as assets]
            [clojure.gdx.asset-manager :as manager]
            [clojure.gdx.files :as files]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.string :as str]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ folder]]
  (bind-root assets/manager (manager/load-all
                             (for [[asset-type exts] [[:sound   #{"wav"}]
                                                      [:texture #{"png" "bmp"}]]
                                   file (map #(str/replace-first % folder "")
                                             (files/recursively-search folder exts))]
                               [file asset-type]))))

(defn dispose [_]
  (disposable/dispose assets/manager))
