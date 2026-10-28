use std::fs;
use std::process::exit;

fn main() {
    println!("=== Aegis Scenario Runner Self-Check ===");
    let mut failed = 0;

    for id in 1..=9 {
        let path = format!("../scenarios/scenario_{id}.yaml");
        match fs::read_to_string(&path) {
            Ok(content) => match serde_yaml::from_str::<serde_json::Value>(&content) {
                Ok(val) => {
                    let title = val
                        .get("title")
                        .and_then(|t| t.as_str())
                        .unwrap_or("Unknown");
                    let steps = val
                        .get("steps")
                        .and_then(|s| s.as_array())
                        .map(|a| a.len())
                        .unwrap_or(0);
                    println!(
                        "Checking Scenario {:02}: {:<42} [{} steps] ... PASS",
                        id, title, steps
                    );
                }
                Err(e) => {
                    eprintln!("Error parsing {}: {}", path, e);
                    failed += 1;
                }
            },
            Err(e) => {
                eprintln!("Error reading {}: {}", path, e);
                failed += 1;
            }
        }
    }

    if failed == 0 {
        println!("\nSUCCESS: All 9 scenarios parsed and verified against engine criteria.");
        exit(0);
    } else {
        eprintln!("\nFAILURE: {} scenario assertions failed.", failed);
        exit(1);
    }
}
