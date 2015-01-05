(defproject jiksnu-atom "0.1.0-SNAPSHOT"
  :description "Atom Module for Jiksnu"
  :url "https://github.com/duck1123/jiksnu-atom"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [

                 [net.kronkltd/jiksnu-core "0.1.0-SNAPSHOT"]
                 [org.clojure/clojure "1.6.0"]
                 ;; [org.slf4j/slf4j-api "1.7.9"]
                 ;; [org.slf4j/slf4j-log4j12 "1.7.9"]

                 ]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  )
