/**
 * @author Sylvain Gougouzian
 * @version 1.0
 * @description Analyse a github project
 */

import { top1000 as topPHPPackages } from "./packagistorg/top1000";
import { top1000 as topJSPackages } from "./npmjscom/top1000";
import { saveJson } from "./utils";

console.log("Start analysis");
console.log("🔍 Top 1000 PHP packages 🐘");
const topPHP = await topPHPPackages();

console.log("🔍 Top 1000 JS packages 🟡");
const topJS = await topJSPackages();

saveJson("outputs/artifact.json", { ...topJS, ...topPHP });
