(ns forge.app.asset-manager
  (:require [anvil.app :as app]
            [clojure.string :as str]
            [clojure.gdx.asset-manager :as manager]
            [clojure.gdx.files :as files]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ folder]]
  (bind-root app/assets (manager/create))
  (manager/load app/assets
                (for [[asset-type exts] [[:sound   #{"wav"}]
                                         [:texture #{"png" "bmp"}]] ; <- this is also data !
                      file (map #(str/replace-first % folder "")
                                (files/recursively-search folder exts))]
                  [file asset-type])))

(defn destroy [_]
  (dispose app/assets))
