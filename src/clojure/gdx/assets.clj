(ns clojure.gdx.assets
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.utils Disposable)))

(defprotocol PAssets
  (all-of-type [_ asset-type]))

(deftype Assets [asset-map]
  PAssets
  (all-of-type [_ asset-type]
    (filter #(= (class %)
                (case asset-type
                  :sound Sound
                  :texture Texture))
            (vals asset-map)))

  clojure.lang.IFn
  (invoke [_ param]
    (asset-map param))

  Disposable
  (dispose [_]
    (println "Disposing assets.")
    (run! Disposable/.dispose (vals asset-map))))

(defn create [assets]
  (->Assets (into {}
                  (for [[file asset-type] assets]
                    [file (case asset-type
                            :sound (.newSound Gdx/audio (.internal Gdx/files file))
                            :texture (Texture. file))]))))
